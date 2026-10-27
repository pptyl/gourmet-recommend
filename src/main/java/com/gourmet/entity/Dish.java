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
 * 菜品。
 */
@Data
@TableName("dish")
@Schema(description = "菜品")
public class Dish implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "所属商家 ID")
    private Long merchantId;

    @Schema(description = "菜名")
    private String name;

    @Schema(description = "售价")
    private BigDecimal price;

    @Schema(description = "菜品介绍")
    private String intro;

    @Schema(description = "菜品图片")
    private String image;

    @Schema(description = "1 上架 0 下架")
    private Integer status;

    @Schema(description = "销量")
    private Integer sales;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @JsonIgnore
    @TableLogic
    private Integer deleted;

    @TableField(exist = false)
    @Schema(description = "所属店铺名称")
    private String merchantName;
}
