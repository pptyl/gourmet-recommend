package com.gourmet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 启用 / 禁用账号参数。
 */
@Data
@Schema(description = "账号状态参数")
public class UserStatusDTO {

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态只能为 0 或 1")
    @Max(value = 1, message = "状态只能为 0 或 1")
    @Schema(description = "1 启用 0 禁用", example = "1")
    private Integer status;
}
