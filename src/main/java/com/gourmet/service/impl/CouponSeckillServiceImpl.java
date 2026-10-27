package com.gourmet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.gourmet.common.constant.BizConstants;
import com.gourmet.common.exception.BusinessException;
import com.gourmet.common.redis.RedisService;
import com.gourmet.common.redis.RedissonLockManager;
import com.gourmet.common.result.ResultCode;
import com.gourmet.config.SeckillProperties;
import com.gourmet.entity.Coupon;
import com.gourmet.entity.UserCoupon;
import com.gourmet.mapper.CouponMapper;
import com.gourmet.mapper.UserCouponMapper;
import com.gourmet.service.CouponSeckillService;
import com.gourmet.vo.SeckillResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 抢券服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponSeckillServiceImpl implements CouponSeckillService {

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final RedisService redisService;
    private final RedissonLockManager redissonLockManager;
    private final SeckillProperties seckillProperties;
    private final DefaultRedisScript<Long> seckillScript;
    private final StringRedisTemplate stringRedisTemplate;

    /** 优惠券本地缓存 */
    private final com.github.benmanes.caffeine.cache.Cache<Long, Coupon> couponCache =
            com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
                    .expireAfterWrite(Duration.ofSeconds(3))
                    .maximumSize(4096)
                    .build();

    private volatile long redisCheckedAt = 0L;
    private volatile boolean redisReady = false;

    @Override
    public SeckillResultVO seckill(Long userId, Long couponId) {
        Coupon coupon = cachedCoupon(couponId);
        if (coupon == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "优惠券不存在");
        }
        checkActivity(coupon);

        // Redis 不可用时降级为数据库扣减
        if (!redisReady()) {
            log.warn("Redis 不可用，优惠券 {} 降级为数据库扣减", couponId);
            String requestId = newRequestId();
            ensureNotClaimed(userId, couponId);
            persistOrder(requestId, userId, coupon, System.currentTimeMillis());
            return SeckillResultVO.done(couponId, requestId, "抢券成功（降级模式）");
        }

        boolean userLocked = false;
        String userLockKey = seckillProperties.userLockKey(couponId, userId);
        try {
            // 用户级幂等锁
            if (seckillProperties.isUserLockEnabled() && redissonLockManager.available()) {
                userLocked = redissonLockManager.tryLock(
                        userLockKey, seckillProperties.getUserLockWaitMs(), seckillProperties.getLockLeaseMs());
                if (!userLocked) {
                    throw new BusinessException(ResultCode.CONFLICT, "请求正在处理中，请勿重复提交");
                }
            }

            // Lua 原子扣减库存
            Long result = redisService.execute(seckillScript,
                    List.of(seckillProperties.stockKey(couponId), seckillProperties.userKey(couponId)),
                    String.valueOf(userId));
            if (result != null && result == BizConstants.SECKILL_NOT_INIT) {
                // 库存未初始化时补库存后重试
                ensureStock(coupon);
                result = redisService.execute(seckillScript,
                        List.of(seckillProperties.stockKey(couponId), seckillProperties.userKey(couponId)),
                        String.valueOf(userId));
            }
            if (result == null) {
                // 脚本异常时降级为数据库扣减
                log.warn("Lua 执行返回空，优惠券 {} 降级数据库扣减", couponId);
                String requestId = newRequestId();
                ensureNotClaimed(userId, couponId);
                persistOrder(requestId, userId, coupon, System.currentTimeMillis());
                return SeckillResultVO.done(couponId, requestId, "抢券成功（降级模式）");
            }
            if (result == BizConstants.SECKILL_DUPLICATE) {
                throw new BusinessException(ResultCode.CONFLICT, "每人限领 1 张，你已领取过该优惠券");
            }
            if (result == BizConstants.SECKILL_NO_STOCK) {
                throw new BusinessException(ResultCode.CONFLICT, "手慢了，优惠券已被抢光");
            }
            if (result == BizConstants.SECKILL_NOT_INIT) {
                throw new BusinessException(ResultCode.CONFLICT, "活动太火爆，请稍后重试");
            }

            String requestId = newRequestId();
            if (seckillProperties.isAsyncEnabled()) {
                // 订单入队后立即返回
                enqueue(requestId, userId, coupon);
                log.debug("抢券入队：user={}, coupon={}, requestId={}", userId, couponId, requestId);
                return SeckillResultVO.queued(couponId, requestId, "抢券成功，正在为你生成券，请稍后到券包查看");
            }

            persistOrder(requestId, userId, coupon, System.currentTimeMillis());
            return SeckillResultVO.done(couponId, requestId, "抢券成功，已放入券包");
        } finally {
            if (userLocked) {
                redissonLockManager.unlock(userLockKey);
            }
        }
    }

    /** 获取优惠券（本地缓存） */
    private Coupon cachedCoupon(Long couponId) {
        return couponCache.get(couponId, couponMapper::selectById);
    }

    /** 判断 Redis 是否可用（带缓存） */
    private boolean redisReady() {
        long now = System.currentTimeMillis();
        if (now - redisCheckedAt > seckillProperties.getRedisCheckIntervalMs()) {
            redisReady = redisService.available();
            redisCheckedAt = now;
        }
        return redisReady;
    }

    @Override
    public SeckillResultVO orderStatus(Long userId, Long couponId) {
        Long count = userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponId, couponId));
        if (count != null && count > 0) {
            return SeckillResultVO.done(couponId, null, "已放入券包");
        }
        Boolean inQueue = redisService.hasKey(seckillProperties.userKey(couponId));
        return SeckillResultVO.queued(couponId, null, Boolean.TRUE.equals(inQueue) ? "正在处理中" : "尚未参与");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleOrder(String requestId, Long userId, Long couponId, long receiveTime) {
        Long exists = userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponId, couponId));
        if (exists != null && exists > 0) {
            markDone(requestId);
            return;
        }
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            log.warn("异步落库失败：优惠券 {} 不存在", couponId);
            return;
        }

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setMerchantId(coupon.getMerchantId());
        userCoupon.setStatus(BizConstants.USER_COUPON_UNUSED);
        userCoupon.setReceiveTime(receiveTime > 0
                ? LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(receiveTime), java.time.ZoneId.systemDefault())
                : LocalDateTime.now());
        userCoupon.setExpireTime(coupon.getValidEndTime() != null
                ? coupon.getValidEndTime() : LocalDateTime.now().plusDays(30));

        int attempts = 0;
        while (true) {
            userCoupon.setRedeemCode(generateRedeemCode());
            try {
                userCouponMapper.insert(userCoupon);
                break;
            } catch (DuplicateKeyException e) {
                Long again = userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getCouponId, couponId));
                if (again != null && again > 0) {
                    markDone(requestId);
                    return;
                }
                if (++attempts >= 3) {
                    throw e;
                }
            }
        }

        couponMapper.update(null, new LambdaUpdateWrapper<Coupon>()
                .eq(Coupon::getId, couponId)
                .gt(Coupon::getRemainStock, 0)
                .setSql("remain_stock = remain_stock - 1"));
        markDone(requestId);
    }

    private void checkActivity(Coupon coupon) {
        if (coupon.getStatus() == null || coupon.getStatus() != BizConstants.COUPON_ON) {
            throw new BusinessException("该优惠券活动已下架");
        }
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getGrabBeginTime() != null && now.isBefore(coupon.getGrabBeginTime())) {
            throw new BusinessException("抢购尚未开始，请稍后再来");
        }
        if (coupon.getGrabEndTime() != null && now.isAfter(coupon.getGrabEndTime())) {
            throw new BusinessException("抢购已结束");
        }
    }

    private void ensureStock(Coupon coupon) {
        String stockKey = seckillProperties.stockKey(coupon.getId());
        if (!redisService.hasKey(stockKey)) {
            int remain = coupon.getRemainStock() == null ? 0 : coupon.getRemainStock();
            redisService.setIfAbsent(stockKey, String.valueOf(remain), ttlOf(coupon));
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

    /** 写订单消息到消息队列 */
    private void enqueue(String requestId, Long userId, Coupon coupon) {
        Map<String, String> message = new LinkedHashMap<>();
        message.put("requestId", requestId);
        message.put("userId", String.valueOf(userId));
        message.put("couponId", String.valueOf(coupon.getId()));
        message.put("merchantId", String.valueOf(coupon.getMerchantId()));
        message.put("receiveTime", String.valueOf(System.currentTimeMillis()));
        stringRedisTemplate.opsForStream().add(StreamRecords.mapBacked(message)
                .withStreamKey(seckillProperties.getStreamKey()));
    }

    /** 校验是否已领取 */
    private void ensureNotClaimed(Long userId, Long couponId) {
        Long exists = userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponId, couponId));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "每人限领 1 张，你已领取过该优惠券");
        }
    }

    /** 同步落库 */
    private void persistOrder(String requestId, Long userId, Coupon coupon, long receiveTime) {
        handleOrder(requestId, userId, coupon.getId(), receiveTime);
    }

    private void markDone(String requestId) {
        if (requestId != null) {
            redisService.set("seckill:order:" + requestId, "DONE", Duration.ofHours(1));
        }
    }

    private String newRequestId() {
        return "R" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /** 生成核销码 */
    private String generateRedeemCode() {
        long value = java.util.concurrent.ThreadLocalRandom.current()
                .nextLong(100_000_000_000L, 1_000_000_000_000L);
        return String.valueOf(value);
    }
}
