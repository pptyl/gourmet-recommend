package com.gourmet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评价点赞记录（user_id + review_id 唯一）。
 */
@Data
@TableName("review_like")
public class ReviewLike implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long reviewId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
