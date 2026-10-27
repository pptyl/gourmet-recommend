package com.gourmet.dto;

import com.gourmet.common.query.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 后台商家列表查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "后台商家查询条件")
public class AdminMerchantQuery extends PageQuery {

    @Schema(description = "店铺名称 / 手机号关键字")
    private String keyword;

    @Schema(description = "审核状态：0 待审核 1 已通过 2 已驳回")
    private Integer auditStatus;

    @Schema(description = "营业状态：1 营业 0 停业")
    private Integer status;
}
