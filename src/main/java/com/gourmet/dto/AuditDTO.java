package com.gourmet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 内容审核参数（评价 / 笔记 / 商家审核通用）。
 */
@Data
@Schema(description = "审核参数")
public class AuditDTO {

    @NotNull(message = "审核结果不能为空")
    @Schema(description = "true 通过，false 驳回", example = "true")
    private Boolean pass;

    @Schema(description = "审核意见")
    private String remark;
}
