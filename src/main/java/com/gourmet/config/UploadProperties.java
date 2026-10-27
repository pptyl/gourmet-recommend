package com.gourmet.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件上传配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {

    /** 本地存储根目录 */
    private String path = "./uploads";

    /** 静态访问前缀 */
    private String urlPrefix = "/uploads";

    /** 允许的扩展名 */
    private String allowedExt = "jpg,jpeg,png,gif,webp,bmp";

    /** 单文件大小上限（MB） */
    private long maxSizeMb = 10;
}
