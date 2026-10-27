package com.gourmet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 想去清单（心愿单）。
 */
@Data
@TableName("wish_list")
@Schema(description = "心愿单")
public class WishList implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long merchantId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "计划打卡日期")
    private LocalDate planDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /* ---------------- 展示字段 ---------------- */

    @TableField(exist = false)
    private String merchantName;

    @TableField(exist = false)
    private String coverImage;

    @TableField(exist = false)
    private String category;

    @TableField(exist = false)
    private String address;

    @TableField(exist = false)
    private BigDecimal score;
}
