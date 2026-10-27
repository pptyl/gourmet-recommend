package com.gourmet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 心愿单新增 / 编辑参数。
 */
@Data
@Schema(description = "心愿单参数")
public class WishListDTO {

    @NotNull(message = "商家 ID 不能为空")
    @Schema(description = "商家 ID", example = "1")
    private Long merchantId;

    @Size(max = 255, message = "备注最多 255 个字符")
    @Schema(description = "备注", example = "周末想去打卡")
    private String remark;

    @Schema(description = "计划打卡日期", example = "2026-06-01")
    private LocalDate planDate;
}
