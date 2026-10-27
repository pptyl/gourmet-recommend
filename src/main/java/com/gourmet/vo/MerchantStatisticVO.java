package com.gourmet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商家数据统计。
 */
@Data
@Schema(description = "商家数据统计")
public class MerchantStatisticVO implements Serializable {

    private Long merchantId;

    @Schema(description = "店铺名称")
    private String name;

    @Schema(description = "综合评分")
    private BigDecimal score;

    @Schema(description = "评价数量")
    private long reviewCount;

    @Schema(description = "收藏量")
    private long favoriteCount;

    @Schema(description = "浏览量")
    private long viewCount;

    @Schema(description = "在售菜品数")
    private long dishCount;

    @Schema(description = "优惠券活动数")
    private long couponCount;

    @Schema(description = "领券总数")
    private long couponClaimCount;
}
