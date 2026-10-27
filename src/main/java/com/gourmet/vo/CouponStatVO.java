package com.gourmet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 优惠券领取统计。
 */
@Data
@Schema(description = "优惠券领取统计")
public class CouponStatVO implements Serializable {

    @Schema(description = "优惠券 ID")
    private Long couponId;

    @Schema(description = "券名称")
    private String title;

    @Schema(description = "总库存")
    private Integer totalStock;

    @Schema(description = "剩余库存")
    private Integer remainStock;

    @Schema(description = "已领取数量")
    private Long claimed;

    @Schema(description = "已使用数量")
    private Long used;
}
