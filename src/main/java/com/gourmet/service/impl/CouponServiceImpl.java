package com.gourmet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gourmet.common.constant.BizConstants;
import com.gourmet.common.context.UserContext;
import com.gourmet.common.exception.BusinessException;
import com.gourmet.common.query.PageQuery;
import com.gourmet.common.redis.RedisService;
import com.gourmet.common.result.PageResult;
import com.gourmet.common.result.ResultCode;
import com.gourmet.config.SeckillProperties;
import com.gourmet.dto.CouponDTO;
import com.gourmet.dto.CouponQuery;
import com.gourmet.entity.Coupon;
import com.gourmet.entity.Favorite;
import com.gourmet.entity.Merchant;
import com.gourmet.entity.User;
import com.gourmet.entity.UserCoupon;
import com.gourmet.mapper.CouponMapper;
import com.gourmet.mapper.FavoriteMapper;
import com.gourmet.mapper.MerchantMapper;
import com.gourmet.mapper.UserMapper;
import com.gourmet.mapper.UserCouponMapper;
import com.gourmet.service.CouponSeckillService;
import com.gourmet.service.CouponService;
import com.gourmet.service.MessageService;
import com.gourmet.vo.CouponStatVO;
import com.gourmet.vo.RedeemResultVO;
import com.gourmet.vo.SeckillResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 优惠券服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final MerchantMapper merchantMapper;
    private final FavoriteMapper favoriteMapper;
    private final UserMapper userMapper;
    private final MessageService messageService;
    private final RedisService redisService;
    private final SeckillProperties seckillProperties;
    private final CouponSeckillService couponSeckillService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Coupon create(Long merchantId, CouponDTO dto) {
        validateTime(dto);

        Coupon coupon = new Coupon();
        coupon.setMerchantId(merchantId);
        applyDto(coupon, dto);
        coupon.setRemainStock(dto.getTotalStock());
        coupon.setStatus(BizConstants.COUPON_ON);
        couponMapper.insert(coupon);

        initRedisStock(coupon, true);
        notifyFavorUsers(coupon);
        log.info("商家 {} 发布优惠券 {}：{}", merchantId, coupon.getId(), coupon.getTitle());
        return coupon;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Coupon update(Long merchantId, Long couponId, CouponDTO dto) {
        Coupon coupon = getOwned(merchantId, couponId);
        validateTime(dto);

        int claimed = (coupon.getTotalStock() == null ? 0 : coupon.getTotalStock())
                - (coupon.getRemainStock() == null ? 0 : coupon.getRemainStock());
        int newRemain = dto.getTotalStock() - claimed;
        if (newRemain < 0) {
            throw new BusinessException("总库存不能小于已领取数量 " + claimed);
        }

        applyDto(coupon, dto);
        coupon.setRemainStock(newRemain);
        couponMapper.updateById(coupon);
        initRedisStock(coupon, false);
        return coupon;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long merchantId, Long couponId, Integer status) {
        if (status == null || (status != BizConstants.COUPON_ON && status != BizConstants.COUPON_OFF)) {
            throw new BusinessException("优惠券状态只能为 0 或 1");
        }
        Coupon coupon = getOwned(merchantId, couponId);
        coupon.setStatus(status);
        couponMapper.updateById(coupon);

        if (status == BizConstants.COUPON_ON) {
            initRedisStock(coupon, false);
        } else {
            redisService.delete(seckillProperties.stockKey(couponId));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long merchantId, Long couponId) {
        getOwned(merchantId, couponId);
        couponMapper.deleteById(couponId);
        redisService.delete(List.of(seckillProperties.stockKey(couponId)));
    }

    @Override
    public PageResult<Coupon> page(CouponQuery query) {
        Page<Coupon> page = query.toPage();
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<Coupon>()
                .eq(query.getMerchantId() != null, Coupon::getMerchantId, query.getMerchantId())
                .eq(query.getStatus() != null, Coupon::getStatus, query.getStatus())
                .like(StringUtils.hasText(query.getKeyword()), Coupon::getTitle, query.getKeyword())
                .orderByDesc(Coupon::getCreateTime);
        Page<Coupon> result = couponMapper.selectPage(page, wrapper);
        result.getRecords().forEach(this::fillState);
        applyRedisStock(result.getRecords());
        return PageResult.of(result);
    }

    @Override
    public Coupon detail(Long couponId, Long userId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "优惠券不存在");
        }
        fillState(coupon);
        applyRedisStock(List.of(coupon));
        Merchant merchant = merchantMapper.selectById(coupon.getMerchantId());
        if (merchant != null) {
            coupon.setMerchantName(merchant.getName());
        }
        if (userId != null) {
            Long count = userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                    .eq(UserCoupon::getUserId, userId)
                    .eq(UserCoupon::getCouponId, couponId));
            coupon.setReceived(count != null && count > 0);
        }
        return coupon;
    }

    @Override
    public List<Coupon> listByMerchant(Long merchantId) {
        List<Coupon> coupons = couponMapper.selectList(new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getMerchantId, merchantId)
                .eq(Coupon::getStatus, BizConstants.COUPON_ON)
                .orderByDesc(Coupon::getCreateTime));
        coupons.forEach(this::fillState);
        applyRedisStock(coupons);
        return coupons;
    }

    @Override
    public PageResult<Coupon> center(Integer state, PageQuery query) {
        LocalDateTime now = LocalDateTime.now();
        Page<Coupon> page = query.toPage();
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getStatus, BizConstants.COUPON_ON)
                .orderByAsc(Coupon::getGrabBeginTime);
        if (state != null && state == 0) {
            // 即将开始
            wrapper.gt(Coupon::getGrabBeginTime, now);
        } else if (state != null && state == 1) {
            // 可领取：已开始、未结束、还有库存
            wrapper.le(Coupon::getGrabBeginTime, now)
                    .gt(Coupon::getGrabEndTime, now)
                    .gt(Coupon::getRemainStock, 0);
        } else if (state != null && state == 2) {
            // 已结束：抢购时间已过（含仍剩库存的券，避免“Redis 有、前台没有”）
            wrapper.lt(Coupon::getGrabEndTime, now);
        }
        Page<Coupon> result = couponMapper.selectPage(page, wrapper);
        if (result.getRecords().isEmpty()) {
            return PageResult.of(result);
        }

        List<Long> merchantIds = result.getRecords().stream()
                .map(Coupon::getMerchantId).distinct().toList();
        Map<Long, String> merchantNames = merchantMapper.selectBatchIds(merchantIds).stream()
                .collect(Collectors.toMap(Merchant::getId, Merchant::getName));

        Long userId = UserContext.getUserIdOrNull();
        Set<Long> received = Set.of();
        if (userId != null) {
            List<Long> couponIds = result.getRecords().stream().map(Coupon::getId).toList();
            received = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                            .eq(UserCoupon::getUserId, userId)
                            .in(UserCoupon::getCouponId, couponIds))
                    .stream().map(UserCoupon::getCouponId).collect(Collectors.toSet());
        }

        for (Coupon coupon : result.getRecords()) {
            coupon.setMerchantName(merchantNames.get(coupon.getMerchantId()));
            fillState(coupon);
            coupon.setReceived(received.contains(coupon.getId()));
        }
        applyRedisStock(result.getRecords());
        return PageResult.of(result);
    }

    @Override
    public SeckillResultVO seckill(Long userId, Long couponId) {
        // 第三 / 四版链路：Redisson 锁 + Lua 原子扣减 + Stream 异步落库
        return couponSeckillService.seckill(userId, couponId);
    }

    @Override
    public List<UserCoupon> myCoupons(Long userId, Integer status) {
        expireUserCoupons(userId);
        return userCouponMapper.selectMyCoupons(userId, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RedeemResultVO redeem(Long merchantId, String code) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException(ResultCode.FORBIDDEN, "当前账号不是商家");
        }
        UserCoupon userCoupon = userCouponMapper.selectOne(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getRedeemCode, code));
        if (userCoupon == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "核销码不存在，请核对后重试");
        }
        if (!merchant.getId().equals(userCoupon.getMerchantId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "该券不属于本店，无法核销");
        }
        if (userCoupon.getStatus() != null && userCoupon.getStatus() == BizConstants.USER_COUPON_USED) {
            throw new BusinessException(ResultCode.CONFLICT, "该券已核销，不能重复使用");
        }
        if ((userCoupon.getStatus() != null && userCoupon.getStatus() == BizConstants.USER_COUPON_EXPIRED)
                || (userCoupon.getExpireTime() != null && userCoupon.getExpireTime().isBefore(LocalDateTime.now()))) {
            throw new BusinessException("该券已过期，无法核销");
        }

        LocalDateTime now = LocalDateTime.now();
        int updated = userCouponMapper.update(null, new LambdaUpdateWrapper<UserCoupon>()
                .eq(UserCoupon::getId, userCoupon.getId())
                .eq(UserCoupon::getStatus, BizConstants.USER_COUPON_UNUSED)
                .set(UserCoupon::getStatus, BizConstants.USER_COUPON_USED)
                .set(UserCoupon::getUseTime, now));
        if (updated == 0) {
            throw new BusinessException(ResultCode.CONFLICT, "该券已被核销，不能重复使用");
        }

        Coupon coupon = couponMapper.selectById(userCoupon.getCouponId());
        User user = userMapper.selectById(userCoupon.getUserId());

        RedeemResultVO vo = new RedeemResultVO();
        vo.setUserCouponId(userCoupon.getId());
        vo.setRedeemCode(userCoupon.getRedeemCode());
        vo.setUseTime(now);
        if (coupon != null) {
            vo.setCouponTitle(coupon.getTitle());
            vo.setThresholdAmount(coupon.getThresholdAmount());
            vo.setDiscountAmount(coupon.getDiscountAmount());
        }
        if (user != null) {
            vo.setUserNickname(user.getNickname());
            vo.setUserPhone(user.getPhone());
        }
        log.info("商家 {} 核销优惠券成功：code={}, userCouponId={}", merchant.getId(), code, userCoupon.getId());
        return vo;
    }

    @Override
    public PageResult<UserCoupon> claimRecords(Long merchantId, Long couponId, PageQuery query) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "优惠券不存在");
        }
        if (!coupon.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能查看本店优惠券的领取记录");
        }
        List<UserCoupon> all = userCouponMapper.selectByCouponWithUser(couponId);
        int size = (int) query.getSize();
        int from = (int) Math.min(all.size(), (query.getPage() - 1) * size);
        int to = Math.min(all.size(), from + size);
        return PageResult.of(all.subList(from, to), all.size(), query.getPage(), size);
    }

    @Override
    public List<CouponStatVO> statistics(Long merchantId) {
        return couponMapper.selectCouponStats(merchantId);
    }

    /* ------------------------------------------------------------------ */
    /*                            内部方法                                 */
    /* ------------------------------------------------------------------ */

    private void initRedisStock(Coupon coupon, boolean resetUsers) {
        if (!seckillProperties.isRedisEnabled()) {
            return;
        }
        String stockKey = seckillProperties.stockKey(coupon.getId());
        int remain = coupon.getRemainStock() == null ? 0 : coupon.getRemainStock();
        redisService.set(stockKey, String.valueOf(remain), ttlOf(coupon));
        if (resetUsers) {
            redisService.delete(seckillProperties.userKey(coupon.getId()));
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

    private void notifyFavorUsers(Coupon coupon) {
        List<Favorite> favorites = favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getMerchantId, coupon.getMerchantId())
                .last("limit 200"));
        if (favorites.isEmpty()) {
            return;
        }
        Merchant merchant = merchantMapper.selectById(coupon.getMerchantId());
        String shopName = merchant == null ? "你收藏的店铺" : merchant.getName();
        favorites.forEach(favorite -> messageService.push(
                favorite.getUserId(),
                BizConstants.RECEIVER_USER,
                BizConstants.MSG_COUPON,
                "优惠券开抢：" + coupon.getTitle(),
                "你收藏的「" + shopName + "」上线了「" + coupon.getTitle() + "」，数量有限，先到先得。",
                coupon.getId()));
    }

    private void expireUserCoupons(Long userId) {
        userCouponMapper.update(null, new LambdaUpdateWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, BizConstants.USER_COUPON_UNUSED)
                .lt(UserCoupon::getExpireTime, LocalDateTime.now())
                .set(UserCoupon::getStatus, BizConstants.USER_COUPON_EXPIRED));
    }

    /**
     * 展示层以 Redis 库存为准：商家后台 / 领券中心看到的剩余量与 Redis 完全一致，
     * 数据库 remain_stock 只作为最终一致的结果。
     */
    private void applyRedisStock(List<Coupon> coupons) {
        if (coupons == null || coupons.isEmpty() || !redisService.available()) {
            return;
        }
        for (Coupon coupon : coupons) {
            String value = redisService.get(seckillProperties.stockKey(coupon.getId()));
            if (value != null) {
                try {
                    coupon.setRemainStock(Integer.parseInt(value));
                } catch (NumberFormatException ignored) {
                    // 非数字视为未初始化，保持数据库值
                }
            }
        }
    }

    private void fillState(Coupon coupon) {
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getStatus() == null || coupon.getStatus() == BizConstants.COUPON_OFF) {
            coupon.setSeckillStatus(4);
            return;
        }
        if (coupon.getGrabBeginTime() != null && now.isBefore(coupon.getGrabBeginTime())) {
            coupon.setSeckillStatus(0);
            coupon.setCountdownSeconds(Duration.between(now, coupon.getGrabBeginTime()).getSeconds());
            return;
        }
        if (coupon.getGrabEndTime() != null && now.isAfter(coupon.getGrabEndTime())) {
            coupon.setSeckillStatus(2);
            return;
        }
        if (coupon.getRemainStock() == null || coupon.getRemainStock() <= 0) {
            coupon.setSeckillStatus(3);
            return;
        }
        coupon.setSeckillStatus(1);
        if (coupon.getGrabEndTime() != null) {
            coupon.setCountdownSeconds(Duration.between(now, coupon.getGrabEndTime()).getSeconds());
        }
    }

    private Coupon getOwned(Long merchantId, Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "优惠券不存在");
        }
        if (!coupon.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能操作本店优惠券");
        }
        return coupon;
    }

    private void applyDto(Coupon coupon, CouponDTO dto) {
        coupon.setTitle(dto.getTitle());
        coupon.setDescription(dto.getDescription());
        coupon.setThresholdAmount(dto.getThresholdAmount());
        coupon.setDiscountAmount(dto.getDiscountAmount());
        coupon.setTotalStock(dto.getTotalStock());
        coupon.setGrabBeginTime(dto.getGrabBeginTime());
        coupon.setGrabEndTime(dto.getGrabEndTime());
        coupon.setValidStartTime(dto.getValidStartTime() == null ? dto.getGrabBeginTime() : dto.getValidStartTime());
        coupon.setValidEndTime(dto.getValidEndTime() == null ? dto.getGrabEndTime().plusDays(30) : dto.getValidEndTime());
        coupon.setPerLimit(dto.getPerLimit() == null ? 1 : dto.getPerLimit());
    }

    private void validateTime(CouponDTO dto) {
        if (dto.getGrabEndTime().isBefore(dto.getGrabBeginTime())
                || dto.getGrabEndTime().isEqual(dto.getGrabBeginTime())) {
            throw new BusinessException("抢购结束时间必须晚于开始时间");
        }
        if (dto.getValidStartTime() != null && dto.getValidEndTime() != null
                && !dto.getValidEndTime().isAfter(dto.getValidStartTime())) {
            throw new BusinessException("券有效期结束时间必须晚于开始时间");
        }
        if (dto.getDiscountAmount().compareTo(dto.getThresholdAmount()) > 0) {
            throw new BusinessException("减免金额不能大于满减门槛");
        }
    }
}
