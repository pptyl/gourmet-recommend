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
import java.util.List;

/**
 * 商家修改店铺资料参数。
 */
@Data
@Schema(description = "修改店铺资料")
public class MerchantUpdateDTO implements Serializable {

    @NotBlank(message = "店铺名称不能为空")
    @Size(max = 100, message = "店铺名称最多 100 个字符")
    @Schema(description = "店铺名称")
    private String name;

    @Pattern(regexp = "^$|^[\\u4e00-\\u9fa5A-Za-z·]{2,10}$",
            message = "菜系只能填写中文或英文名称（2-10 个字）")
    @Schema(description = "菜系")
    private String category;

    @Pattern(regexp = "^$|^[\\u4e00-\\u9fa5A-Za-z·]{2,20}$",
            message = "地区只能填写中文或英文名称（2-20 个字）")
    @Schema(description = "所属地区")
    private String area;

    @Pattern(regexp = "^$|^(?=.*[\\u4e00-\\u9fa5]).{5,255}$",
            message = "详细地址至少 5 个字，并包含中文门牌信息")
    @Schema(description = "详细地址")
    private String address;

    @Size(max = 1000, message = "简介最多 1000 个字符")
    @Schema(description = "店铺简介")
    private String intro;

    @Schema(description = "店铺封面图")
    private String coverImage;

    @Schema(description = "店铺相册（多张图片地址）")
    private List<String> album;

    @Pattern(regexp = "^$|^([01]?\\d|2[0-3]):[0-5]\\d\\s*[-~至]\\s*([01]?\\d|2[0-3]):[0-5]\\d$",
            message = "营业时间格式应为 10:00-22:00")
    @Schema(description = "营业时间")
    private String businessHours;

    @DecimalMin(value = "0", message = "人均消费不能为负")
    @DecimalMax(value = "99999", message = "人均消费不能超过 99999")
    @Schema(description = "人均消费")
    private BigDecimal avgPrice;
}
