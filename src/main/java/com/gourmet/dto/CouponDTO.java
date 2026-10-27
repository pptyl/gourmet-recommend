package com.gourmet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建 / 编辑优惠券参数。
 */
@Data
@Schema(description = "优惠券参数")
public class CouponDTO implements Serializable {

    @NotBlank(message = "券名称不能为空")
    @Size(max = 100, message = "券名称最多 100 个字符")
    @Schema(description = "券名称", example = "满100减30")
    private String title;

    @Size(max = 500, message = "说明最多 500 个字符")
    @Schema(description = "使用说明")
    private String description;

    @NotNull(message = "满减门槛不能为空")
    @DecimalMin(value = "0.01", message = "满减门槛必须大于 0")
    @Schema(description = "满减门槛", example = "100.00")
    private BigDecimal thresholdAmount;

    @NotNull(message = "减免金额不能为空")
    @DecimalMin(value = "0.01", message = "减免金额必须大于 0")
    @Schema(description = "减免金额", example = "30.00")
    private BigDecimal discountAmount;

    @NotNull(message = "库存不能为空")
    @Min(value = 1, message = "库存至少为 1")
    @Schema(description = "总库存", example = "100")
    private Integer totalStock;

    @NotNull(message = "抢购开始时间不能为空")
    @Schema(description = "抢购开始时间", example = "2026-01-01 00:00:00")
    private LocalDateTime grabBeginTime;

    @NotNull(message = "抢购结束时间不能为空")
    @Schema(description = "抢购结束时间", example = "2026-01-31 23:59:59")
    private LocalDateTime grabEndTime;

    @Schema(description = "券有效期开始", example = "2026-01-01 00:00:00")
    private LocalDateTime validStartTime;

    @Schema(description = "券有效期结束", example = "2026-03-01 00:00:00")
    private LocalDateTime validEndTime;

    @Min(value = 1, message = "每人限领至少 1 张")
    @Schema(description = "每人限领数量", example = "1")
    private Integer perLimit = 1;
}
