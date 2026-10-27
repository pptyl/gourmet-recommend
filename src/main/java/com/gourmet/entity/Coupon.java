package com.gourmet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商家优惠券。
 */
@Data
@TableName("coupon")
@Schema(description = "优惠券")
public class Coupon implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "发布商家 ID")
    private Long merchantId;

    @Schema(description = "券名称")
    private String title;

    @Schema(description = "使用说明")
    private String description;

    @Schema(description = "满减门槛")
    private BigDecimal thresholdAmount;

    @Schema(description = "减免金额")
    private BigDecimal discountAmount;

    @Schema(description = "总库存")
    private Integer totalStock;

    @Schema(description = "剩余库存")
    private Integer remainStock;

    @Schema(description = "抢购开始时间")
    private LocalDateTime grabBeginTime;

    @Schema(description = "抢购结束时间")
    private LocalDateTime grabEndTime;

    @Schema(description = "券有效期开始")
    private LocalDateTime validStartTime;

    @Schema(description = "券有效期结束")
    private LocalDateTime validEndTime;

    @Schema(description = "每人限领数量")
    private Integer perLimit;

    @Schema(description = "1 上架 0 下架")
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @JsonIgnore
    @TableLogic
    private Integer deleted;

    /* ---------------- 展示字段 ---------------- */

    @TableField(exist = false)
    @Schema(description = "店铺名称")
    private String merchantName;

    @TableField(exist = false)
    @Schema(description = "抢购状态：0 未开始 1 进行中 2 已结束 3 已抢光 4 已下架")
    private Integer seckillStatus;

    @TableField(exist = false)
    @Schema(description = "当前用户是否已领取")
    private Boolean received;

    @TableField(exist = false)
    @Schema(description = "距离开始/结束的秒数")
    private Long countdownSeconds;
}
