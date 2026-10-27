package com.gourmet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 用户图文评价。
 */
@Data
@TableName("review")
@Schema(description = "评价")
public class Review implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "评价用户 ID")
    private Long userId;

    @Schema(description = "商家 ID")
    private Long merchantId;

    @Schema(description = "菜品 ID（可空）")
    private Long dishId;

    @Schema(description = "星级 1-5")
    private Integer score;

    @Schema(description = "评价内容")
    private String content;

    @Schema(description = "图片地址，逗号分隔")
    private String images;

    @Schema(description = "0 待审核 1 已通过 2 违规下架")
    private Integer status;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String userName;

    @TableField(exist = false)
    private String userAvatar;

    @TableField(exist = false)
    private String merchantName;

    @TableField(exist = false)
    private String dishName;

    @TableField(exist = false)
    @Schema(description = "当前用户是否已点赞")
    private Boolean liked;

    /**
     * 图片列表视图（供前端直接使用）。
     */
    public List<String> getImageList() {
        if (images == null || images.isBlank()) {
            return List.of();
        }
        return Arrays.stream(images.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
