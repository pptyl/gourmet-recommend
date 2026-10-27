package com.gourmet.dto;

import com.gourmet.common.query.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 菜品查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "菜品查询条件")
public class DishQuery extends PageQuery {

    @Schema(description = "商家 ID")
    private Long merchantId;

    @Schema(description = "菜名关键字")
    private String keyword;

    @Schema(description = "1 仅看上架 0 仅看下架，不传为全部")
    private Integer status;
}
