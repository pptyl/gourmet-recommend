package com.gourmet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 手机号 + 密码登录参数（用户 / 商家通用）。
 */
@Data
@Schema(description = "登录参数")
public class LoginDTO {

    @NotBlank(message = "账号不能为空")
    @Schema(description = "手机号", example = "13800000001")
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", example = "123456")
    private String password;
}
