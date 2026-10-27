package com.gourmet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员消息群发参数。
 */
@Data
@Schema(description = "消息群发参数")
public class BroadcastDTO {

    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题最多 100 个字符")
    @Schema(description = "标题", example = "平台活动通知")
    private String title;

    @Size(max = 1000, message = "内容最多 1000 个字符")
    @Schema(description = "内容")
    private String content;

    @Schema(description = "接收者类型：1 用户 2 商家，默认 1", example = "1")
    private Integer receiverType = 1;

    @Schema(description = "消息类型：1 新店开业 2 新品 3 优惠券 4 系统 5 审核，默认 4", example = "4")
    private Integer type = 4;
}
