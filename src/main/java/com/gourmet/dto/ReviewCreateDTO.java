package com.gourmet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 发布评价参数。
 */
@Data
@Schema(description = "发布评价参数")
public class ReviewCreateDTO implements Serializable {

    @NotNull(message = "商家 ID 不能为空")
    @Schema(description = "商家 ID", example = "1")
    private Long merchantId;

    @Schema(description = "菜品 ID（可选）", example = "1")
    private Long dishId;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低 1 星")
    @Max(value = 5, message = "评分最高 5 星")
    @Schema(description = "星级 1-5", example = "5")
    private Integer score;

    @Size(max = 1000, message = "评价内容最多 1000 个字符")
    @Schema(description = "评价内容")
    private String content;

    @Schema(description = "图片地址列表（最多 9 张）")
    private List<String> images;
}
