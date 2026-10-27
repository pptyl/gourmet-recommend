package com.gourmet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户券包中的券。
 */
@Data
@TableName("user_coupon")
@Schema(description = "用户券包")
public class UserCoupon implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long couponId;

    private Long merchantId;

    @Schema(description = "0 未使用 1 已使用 2 已过期")
    private Integer status;

    @Schema(description = "核销码（到店出示，商家输入核销）")
    private String redeemCode;

    @Schema(description = "领取时间")
    private LocalDateTime receiveTime;

    @Schema(description = "使用时间")
    private LocalDateTime useTime;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    /* ---------------- 展示字段 ---------------- */

    @TableField(exist = false)
    private String couponTitle;

    @TableField(exist = false)
    private BigDecimal thresholdAmount;

    @TableField(exist = false)
    private BigDecimal discountAmount;

    @TableField(exist = false)
    private String merchantName;

    @TableField(exist = false)
    private LocalDateTime validStartTime;

    @TableField(exist = false)
    private LocalDateTime validEndTime;

    @TableField(exist = false)
    @Schema(description = "领取人昵称（商家查看领券记录用）")
    private String userNickname;

    @TableField(exist = false)
    @Schema(description = "领取人手机号")
    private String userPhone;
}
