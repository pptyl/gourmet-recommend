package com.gourmet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 推荐反馈参数。
 */
@Data
@Schema(description = "推荐反馈参数")
public class FeedbackDTO {

    @NotNull(message = "商家 ID 不能为空")
    @Schema(description = "商家 ID", example = "1")
    private Long merchantId;

    @NotNull(message = "反馈类型不能为空")
    @Min(value = 1, message = "反馈类型只能为 1 或 2")
    @Max(value = 2, message = "反馈类型只能为 1 或 2")
    @Schema(description = "1 喜欢 2 不感兴趣", example = "1")
    private Integer feedbackType;

    @Schema(description = "来源：HOME/SIMILAR/RANK", example = "HOME")
    private String source;
}
