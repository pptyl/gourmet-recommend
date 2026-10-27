package com.gourmet.dto;

import com.gourmet.common.query.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 优惠券查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "优惠券查询条件")
public class CouponQuery extends PageQuery {

    @Schema(description = "商家 ID")
    private Long merchantId;

    @Schema(description = "1 上架 0 下架")
    private Integer status;

    @Schema(description = "券名称关键字")
    private String keyword;
}
