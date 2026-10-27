package com.gourmet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 商家核销优惠券请求。
 */
@Data
@Schema(description = "核销请求")
public class RedeemDTO {

    @Schema(description = "核销码（券包中展示的 12 位数字）", example = "482913746502")
    @NotBlank(message = "请输入核销码")
    @Pattern(regexp = "^[A-Za-z0-9]{6,32}$", message = "核销码格式不正确")
    private String code;
}
