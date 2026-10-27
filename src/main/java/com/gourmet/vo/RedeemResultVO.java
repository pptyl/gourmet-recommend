package com.gourmet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商家核销结果。
 */
@Data
@Schema(description = "核销结果")
public class RedeemResultVO implements Serializable {

    @Schema(description = "用户券 id")
    private Long userCouponId;

    @Schema(description = "核销码")
    private String redeemCode;

    @Schema(description = "优惠券名称")
    private String couponTitle;

    @Schema(description = "满减门槛")
    private BigDecimal thresholdAmount;

    @Schema(description = "减免金额")
    private BigDecimal discountAmount;

    @Schema(description = "领取人昵称")
    private String userNickname;

    @Schema(description = "领取人手机号")
    private String userPhone;

    @Schema(description = "核销时间")
    private LocalDateTime useTime;
}
