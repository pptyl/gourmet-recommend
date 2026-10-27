package com.gourmet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商家入驻申请参数。
 */
@Data
@Schema(description = "商家入驻申请")
public class MerchantApplyDTO implements Serializable {

    @NotBlank(message = "店铺名称不能为空")
    @Size(max = 100, message = "店铺名称最多 100 个字符")
    @Schema(description = "店铺名称", example = "甜蜜烘焙坊")
    private String name;

    @NotBlank(message = "登录手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "登录手机号", example = "13900000010")
    private String phone;

    @NotBlank(message = "登录密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度需为 6-20 位")
    @Schema(description = "登录密码", example = "123456")
    private String password;

    @NotBlank(message = "菜系不能为空")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5A-Za-z·]{2,10}$",
            message = "菜系只能填写中文或英文名称（2-10 个字），例如：川菜、甜品")
    @Schema(description = "菜系", example = "甜品")
    private String category;

    @NotBlank(message = "地区不能为空")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5A-Za-z·]{2,20}$",
            message = "地区只能填写中文或英文名称（2-20 个字），例如：上海徐汇区")
    @Schema(description = "所属地区", example = "上海徐汇区")
    private String area;

    @NotBlank(message = "详细地址不能为空")
    @Size(min = 5, max = 255, message = "详细地址至少 5 个字")
    @Pattern(regexp = ".*[\\u4e00-\\u9fa5].*", message = "详细地址需包含中文门牌信息，例如：上海市徐汇区田林路 20 号")
    @Schema(description = "详细地址")
    private String address;

    @Size(max = 1000, message = "简介最多 1000 个字符")
    @Schema(description = "店铺简介")
    private String intro;

    @Pattern(regexp = "^$|^([01]?\\d|2[0-3]):[0-5]\\d\\s*[-~至]\\s*([01]?\\d|2[0-3]):[0-5]\\d$",
            message = "营业时间格式应为 10:00-22:00")
    @Schema(description = "营业时间", example = "08:00-20:00")
    private String businessHours;

    @DecimalMin(value = "0", message = "人均消费不能为负")
    @DecimalMax(value = "99999", message = "人均消费不能超过 99999")
    @Schema(description = "人均消费")
    private BigDecimal avgPrice;
}
