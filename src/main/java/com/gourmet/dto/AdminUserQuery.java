package com.gourmet.dto;

import com.gourmet.common.query.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 后台用户列表查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "后台用户查询条件")
public class AdminUserQuery extends PageQuery {

    @Schema(description = "昵称 / 手机号关键字")
    private String keyword;

    @Schema(description = "1 正常 0 禁用")
    private Integer status;
}
