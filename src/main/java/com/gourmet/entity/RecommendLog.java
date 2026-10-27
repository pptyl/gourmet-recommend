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
 * 推荐日志：记录每一次推送的商家与算法。
 */
@Data
@TableName("recommend_log")
@Schema(description = "推荐日志")
public class RecommendLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long merchantId;

    @Schema(description = "算法标识")
    private String algorithm;

    @Schema(description = "推荐得分")
    private Double score;

    @Schema(description = "推荐理由")
    private String reason;

    @Schema(description = "推荐批次号")
    private String requestId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String merchantName;
}
