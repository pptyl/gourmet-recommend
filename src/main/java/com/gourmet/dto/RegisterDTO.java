package com.gourmet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册参数。
 */
@Data
@Schema(description = "用户注册参数")
public class RegisterDTO {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号", example = "13800000009")
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度需为 6-20 位")
    @Schema(description = "密码", example = "123456")
    private String password;

    @Size(max = 30, message = "昵称最多 30 个字符")
    @Schema(description = "昵称", example = "新用户")
    private String nickname;

    @Pattern(regexp = "^$|^[\\u4e00-\\u9fa5A-Za-z·]{2,10}([,，][\\u4e00-\\u9fa5A-Za-z·]{2,10})*$",
            message = "菜系请填写中文名称，多个用逗号分隔，例如：川菜,粤菜")
    @Schema(description = "偏好菜系，逗号分隔", example = "川菜,粤菜")
    private String tasteCuisine;

    @Pattern(regexp = "^$|^(清淡|微辣|中辣|重辣)$",
            message = "口味轻重只能选择：清淡 / 微辣 / 中辣 / 重辣")
    @Schema(description = "口味轻重", example = "微辣")
    private String tasteFlavor;

    @Schema(description = "忌口食材", example = "香菜")
    private String taboo;
}
