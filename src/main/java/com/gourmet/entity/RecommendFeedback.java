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

/**
 * 推荐反馈：喜欢 / 不感兴趣。
 */
@Data
@TableName("recommend_feedback")
@Schema(description = "推荐反馈")
public class RecommendFeedback implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long merchantId;

    @Schema(description = "1 喜欢 2 不感兴趣")
    private Integer feedbackType;

    @Schema(description = "来源：HOME/SIMILAR/RANK")
    private String source;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String merchantName;

    @TableField(exist = false)
    private String coverImage;
}
