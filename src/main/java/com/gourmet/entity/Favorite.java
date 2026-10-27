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
import java.time.LocalDateTime;

/**
 * 店铺收藏。
 */
@Data
@TableName("favorite")
@Schema(description = "店铺收藏")
public class Favorite implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long merchantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /* ---------------- 展示字段 ---------------- */

    @TableField(exist = false)
    private String merchantName;

    @TableField(exist = false)
    private String coverImage;

    @TableField(exist = false)
    private String category;

    @TableField(exist = false)
    private String area;

    @TableField(exist = false)
    private String address;

    @TableField(exist = false)
    private BigDecimal score;

    @TableField(exist = false)
    private BigDecimal avgPrice;
}
