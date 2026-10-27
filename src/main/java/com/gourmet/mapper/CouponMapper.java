package com.gourmet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gourmet.entity.Coupon;
import com.gourmet.vo.CouponStatVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 优惠券 Mapper。
 */
public interface CouponMapper extends BaseMapper<Coupon> {

    /**
     * 券领取数据统计（可按商家过滤）。
     */
    @Select("""
            <script>
            select c.id as coupon_id, c.title as title, c.total_stock as total_stock,
                   c.remain_stock as remain_stock,
                   count(uc.id) as claimed,
                   coalesce(sum(case when uc.status = 1 then 1 else 0 end), 0) as used
            from coupon c
            left join user_coupon uc on uc.coupon_id = c.id
            where c.deleted = 0
            <if test="merchantId != null"> and c.merchant_id = #{merchantId} </if>
            group by c.id, c.title, c.total_stock, c.remain_stock
            order by claimed desc
            </script>
            """)
    List<CouponStatVO> selectCouponStats(@Param("merchantId") Long merchantId);
}
