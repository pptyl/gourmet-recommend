package com.gourmet.common.util;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /** 签名密钥，长度不少于 32 字节 */
    private String secret = "gourmet-recommend-default-secret-key-change-me";

    /** 令牌有效期（分钟） */
    private long expireMinutes = 720;

    /** 令牌请求头 */
    private String header = "Authorization";

    /** 令牌前缀 */
    private String prefix = "Bearer ";
}
