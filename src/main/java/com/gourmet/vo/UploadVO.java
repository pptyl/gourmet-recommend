package com.gourmet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * 文件上传结果。
 */
@Data
@AllArgsConstructor
@Schema(description = "文件上传结果")
public class UploadVO implements Serializable {

    @Schema(description = "访问地址")
    private String url;

    @Schema(description = "原始文件名")
    private String fileName;

    @Schema(description = "文件大小（字节）")
    private long size;
}
