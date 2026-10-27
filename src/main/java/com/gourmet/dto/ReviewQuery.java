package com.gourmet.dto;

import com.gourmet.common.query.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 评价查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "评价查询条件")
public class ReviewQuery extends PageQuery {

    @Schema(description = "商家 ID")
    private Long merchantId;

    @Schema(description = "审核状态：0 待审核 1 已通过 2 违规")
    private Integer status;

    @Schema(description = "内容关键字")
    private String keyword;
}
