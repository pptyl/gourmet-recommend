package com.gourmet.dto;

import com.gourmet.common.query.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 商家列表查询条件（前台）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "商家列表查询条件")
public class MerchantQuery extends PageQuery {

    @Schema(description = "地区，模糊匹配")
    private String area;

    @Schema(description = "菜系")
    private String category;

    @Schema(description = "最低评分")
    private BigDecimal minScore;

    @Schema(description = "店铺名称关键字")
    private String keyword;

    @Schema(description = "排序方式：score 评分 / favorite 收藏 / hot 热度 / new 最新", example = "score")
    private String sort;
}
