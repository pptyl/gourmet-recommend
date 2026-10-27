package com.gourmet.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 安全相关配置（免登录路径白名单）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    /** 免登录访问路径（Ant 风格），可在 application.yml 中覆盖 */
    private List<String> permitAll = new ArrayList<>();
}
