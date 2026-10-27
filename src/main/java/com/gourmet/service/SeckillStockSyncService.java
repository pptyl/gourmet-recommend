package com.gourmet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.gourmet.common.constant.BizConstants;
import com.gourmet.common.redis.RedisService;
import com.gourmet.config.SeckillProperties;
import com.gourmet.entity.Coupon;
import com.gourmet.entity.UserCoupon;
import com.gourmet.mapper.CouponMapper;
import com.gourmet.mapper.UserCouponMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.stream.PendingMessagesSummary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 优惠券库存对账服务：保持数据库与 Redis 库存一致。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillStockSyncService {

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final RedisService redisService;
    private final SeckillProperties seckillProperties;
    private final StringRedisTemplate stringRedisTemplate;

    /** 应用启动后执行一次对账 */
    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        try {
            int fixed = syncAll(true);
            if (fixed >= 0) {
                log.info("优惠券库存对账完成：修正 {} 张券", fixed);
            }
        } catch (Exception e) {
            log.warn("启动库存对账跳过：{}", e.getMessage());
        }
    }

    /** 定时对账 */
    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void scheduledSync() {
        try {
            syncAll(false);
        } catch (Exception e) {
            log.warn("定时库存对账失败：{}", e.getMessage());
        }
    }

    /**
     * 对账全部上架优惠券。
     *
     * @param force 是否忽略在途订单
     * @return 修正的券数量；Redis 不可用返回 -1
     */
    public int syncAll(boolean force) {
        if (!redisService.available()) {
            return -1;
        }
        if (!force && hasPendingOrders()) {
            log.debug("订单队列仍有未消费消息，本轮对账跳过");
            return 0;
        }

        List<Coupon> coupons = couponMapper.selectList(new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getStatus, BizConstants.COUPON_ON));
        int fixed = 0;
        for (Coupon coupon : coupons) {
            try {
                if (syncOne(coupon)) {
                    fixed++;
                }
            } catch (Exception e) {
                log.warn("优惠券 {} 库存对账失败：{}", coupon.getId(), e.getMessage());
            }
        }
        return fixed;
    }

    /** 对账单张优惠券 */
    private boolean syncOne(Coupon coupon) {
        long claimed = userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getCouponId, coupon.getId()));
        int total = coupon.getTotalStock() == null ? 0 : coupon.getTotalStock();
        int expected = (int) Math.max(0, total - claimed);
        boolean changed = false;

        if (coupon.getRemainStock() == null || coupon.getRemainStock() != expected) {
            couponMapper.update(null, new LambdaUpdateWrapper<Coupon>()
                    .eq(Coupon::getId, coupon.getId())
                    .set(Coupon::getRemainStock, expected));
            changed = true;
        }

        String stockKey = seckillProperties.stockKey(coupon.getId());
        String redisValue = redisService.get(stockKey);
        if (redisValue == null || !redisValue.equals(String.valueOf(expected))) {
            redisService.set(stockKey, String.valueOf(expected), ttlOf(coupon));
            changed = true;
        }

        List<Long> userIds = userCouponMapper.selectUserIds(coupon.getId());
        String userKey = seckillProperties.userKey(coupon.getId());
        stringRedisTemplate.delete(userKey);
        if (!userIds.isEmpty()) {
            stringRedisTemplate.opsForSet().add(userKey,
                    userIds.stream().map(String::valueOf).toArray(String[]::new));
            stringRedisTemplate.expire(userKey, ttlOf(coupon));
        }
        return changed;
    }

    /** 是否存在未确认的订单消息 */
    private boolean hasPendingOrders() {
        try {
            PendingMessagesSummary summary = stringRedisTemplate.opsForStream()
                    .pending(seckillProperties.getStreamKey(), seckillProperties.getConsumerGroup());
            return summary != null && summary.getTotalPendingMessages() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private Duration ttlOf(Coupon coupon) {
        LocalDateTime end = coupon.getGrabEndTime();
        if (end == null) {
            return Duration.ofDays(30);
        }
        Duration duration = Duration.between(LocalDateTime.now(), end.plusDays(1));
        return duration.isNegative() || duration.isZero() ? Duration.ofHours(1) : duration;
    }
}
