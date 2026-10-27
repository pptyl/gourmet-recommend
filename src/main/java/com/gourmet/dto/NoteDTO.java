package com.gourmet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 发布 / 编辑探店笔记参数。
 */
@Data
@Schema(description = "探店笔记参数")
public class NoteDTO implements Serializable {

    @Schema(description = "关联商家 ID", example = "1")
    private Long merchantId;

    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题最多 100 个字符")
    @Schema(description = "标题")
    private String title;

    @NotBlank(message = "正文不能为空")
    @Size(max = 4000, message = "正文最多 4000 个字符")
    @Schema(description = "正文")
    private String content;

    @Schema(description = "图片地址列表")
    private List<String> images;

    @Schema(description = "封面图")
    private String coverImage;
}
