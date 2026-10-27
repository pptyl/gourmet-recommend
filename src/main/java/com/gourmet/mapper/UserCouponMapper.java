package com.gourmet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gourmet.entity.UserCoupon;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户券包 Mapper（含多表联查）。
 */
public interface UserCouponMapper extends BaseMapper<UserCoupon> {

    /**
     * 我的券包：关联券信息与店铺名称。
     */
    @Select("""
            <script>
            select uc.*, c.title as coupon_title, c.threshold_amount as threshold_amount,
                   c.discount_amount as discount_amount, c.valid_start_time as valid_start_time,
                   c.valid_end_time as valid_end_time, m.name as merchant_name
            from user_coupon uc
            left join coupon c on c.id = uc.coupon_id
            left join merchant m on m.id = uc.merchant_id
            where uc.user_id = #{userId}
            <if test="status != null"> and uc.status = #{status} </if>
            order by uc.receive_time desc
            </script>
            """)
    List<UserCoupon> selectMyCoupons(@Param("userId") Long userId, @Param("status") Integer status);

    /**
     * 某张券的领取记录（商家后台用）。
     */
    @Select("""
            select uc.*, c.title as coupon_title, u.nickname as user_nickname, u.phone as user_phone
            from user_coupon uc
            left join coupon c on c.id = uc.coupon_id
            left join sys_user u on u.id = uc.user_id
            where uc.coupon_id = #{couponId}
            order by uc.receive_time desc
            """)
    List<UserCoupon> selectByCouponWithUser(@Param("couponId") Long couponId);

    /**
     * 商家维度领券总数。
     */
    @Select("select count(*) from user_coupon where merchant_id = #{merchantId}")
    long countByMerchant(@Param("merchantId") Long merchantId);

    /**
     * 某张券已领取的用户 id（库存对账时重建 Redis 用户集合）。
     */
    @Select("select user_id from user_coupon where coupon_id = #{couponId}")
    List<Long> selectUserIds(@Param("couponId") Long couponId);
}
