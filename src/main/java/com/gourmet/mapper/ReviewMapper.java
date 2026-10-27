package com.gourmet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gourmet.entity.Review;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 评价 Mapper。
 */
public interface ReviewMapper extends BaseMapper<Review> {

    /**
     * 商家已通过审核评价的平均分（无评价返回 null）。
     */
    @Select("select avg(score) from review where merchant_id = #{merchantId} and status = 1")
    Double selectAvgScore(@Param("merchantId") Long merchantId);
}
