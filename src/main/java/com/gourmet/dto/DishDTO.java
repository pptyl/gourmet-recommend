package com.gourmet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 新增 / 编辑菜品参数。
 */
@Data
@Schema(description = "菜品参数")
public class DishDTO implements Serializable {

    @NotBlank(message = "菜名不能为空")
    @Size(max = 100, message = "菜名最多 100 个字符")
    @Schema(description = "菜名", example = "招牌水煮鱼")
    private String name;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于 0")
    @Schema(description = "售价", example = "88.00")
    private BigDecimal price;

    @Size(max = 1000, message = "介绍最多 1000 个字符")
    @Schema(description = "菜品介绍")
    private String intro;

    @Schema(description = "菜品图片地址")
    private String image;
}
