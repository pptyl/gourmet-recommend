package com.gourmet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 口味偏好设置：菜系、口味轻重、忌口食材。
 */
@Data
@Schema(description = "口味偏好参数")
public class TastePreferenceDTO {

    @Size(max = 100, message = "菜系最多 100 个字符")
    @Pattern(regexp = "^$|^[\\u4e00-\\u9fa5A-Za-z·]{2,10}([,，][\\u4e00-\\u9fa5A-Za-z·]{2,10})*$",
            message = "菜系请填写中文名称，多个用逗号分隔，例如：川菜,湘菜")
    @Schema(description = "偏好菜系，逗号分隔", example = "川菜,湘菜")
    private String tasteCuisine;

    @Pattern(regexp = "^$|^(清淡|微辣|中辣|重辣)$",
            message = "口味轻重只能选择：清淡 / 微辣 / 中辣 / 重辣")
    @Schema(description = "口味轻重：清淡/微辣/中辣/重辣", example = "重辣")
    private String tasteFlavor;

    @Size(max = 200, message = "忌口食材最多 200 个字符")
    @Schema(description = "忌口食材，逗号分隔", example = "香菜,折耳根")
    private String taboo;
}
