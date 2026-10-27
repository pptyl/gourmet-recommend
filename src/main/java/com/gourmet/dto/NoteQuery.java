package com.gourmet.dto;

import com.gourmet.common.query.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 笔记查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "笔记查询条件")
public class NoteQuery extends PageQuery {

    @Schema(description = "商家 ID")
    private Long merchantId;

    @Schema(description = "作者 ID")
    private Long userId;

    @Schema(description = "状态：0 待审核 1 已发布 2 已下架")
    private Integer status;

    @Schema(description = "标题关键字")
    private String keyword;
}
