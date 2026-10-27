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
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * UGC 探店笔记。
 */
@Data
@TableName("note")
@Schema(description = "探店笔记")
public class Note implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "作者 ID")
    private Long userId;

    @Schema(description = "关联商家 ID")
    private Long merchantId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "正文")
    private String content;

    @Schema(description = "图片地址，逗号分隔")
    private String images;

    @Schema(description = "封面图")
    private String coverImage;

    @Schema(description = "浏览量")
    private Integer viewCount;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "0 待审核 1 已发布 2 已下架")
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
    private String userName;

    @TableField(exist = false)
    private String userAvatar;

    @TableField(exist = false)
    private String merchantName;

    @TableField(exist = false)
    private String merchantAddress;

    @TableField(exist = false)
    @Schema(description = "当前用户是否已点赞")
    private Boolean liked;

    public List<String> getImageList() {
        if (images == null || images.isBlank()) {
            return List.of();
        }
        return Arrays.stream(images.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
