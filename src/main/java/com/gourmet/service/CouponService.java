package com.gourmet.service;

import com.gourmet.common.query.PageQuery;
import com.gourmet.common.result.PageResult;
import com.gourmet.dto.CouponDTO;
import com.gourmet.dto.CouponQuery;
import com.gourmet.entity.Coupon;
import com.gourmet.entity.UserCoupon;
import com.gourmet.vo.CouponStatVO;
import com.gourmet.vo.RedeemResultVO;
import com.gourmet.vo.SeckillResultVO;

import java.util.List;

/**
 * 优惠券发布、限时抢购与券包管理。
 */
public interface CouponService {

    /** 商家创建优惠券 */
    Coupon create(Long merchantId, CouponDTO dto);

    /** 商家编辑优惠券 */
    Coupon update(Long merchantId, Long couponId, CouponDTO dto);

    /** 上架 / 下架 */
    void updateStatus(Long merchantId, Long couponId, Integer status);

    /** 删除优惠券 */
    void delete(Long merchantId, Long couponId);

    /** 优惠券分页（商家 / 后台） */
    PageResult<Coupon> page(CouponQuery query);

    /** 优惠券详情（含抢购状态、倒计时、是否已领取） */
    Coupon detail(Long couponId, Long userId);

    /** 某商家进行中的优惠券（前台展示） */
    List<Coupon> listByMerchant(Long merchantId);

    /**
     * 领券中心：全平台可领取 / 即将开始的优惠券。
     *
     * @param state 1 可领取，0 即将开始，null 全部
     */
    PageResult<Coupon> center(Integer state, PageQuery query);

    /** 限时抢券：Redisson 锁 + Lua 原子扣减 + Stream 异步落库 */
    SeckillResultVO seckill(Long userId, Long couponId);

    /** 我的券包，status 为空查全部 */
    List<UserCoupon> myCoupons(Long userId, Integer status);

    /** 商家凭核销码核销优惠券 */
    RedeemResultVO redeem(Long merchantId, String code);

    /** 商家查看某张券的领取记录 */
    PageResult<UserCoupon> claimRecords(Long merchantId, Long couponId, PageQuery query);

    /** 券领取数据统计 */
    List<CouponStatVO> statistics(Long merchantId);
}
