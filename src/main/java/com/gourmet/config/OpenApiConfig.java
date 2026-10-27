package com.gourmet.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger 文档配置。
 *
 * <p>访问地址：http://localhost:8080/swagger-ui.html
 * 点击右上角 Authorize，输入登录接口返回的 token 即可调试需要鉴权的接口。
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "BearerAuth";

    @Bean
    public OpenAPI gourmetOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("美食推荐系统 API")
                        .version("1.0.0")
                        .description("""
                                SpringBoot 美食推荐系统后端接口文档。

                                角色说明：USER 普通用户 / MERCHANT 商家 / ADMIN 平台管理员。
                                鉴权方式：登录接口获取 token，请求头携带 Authorization: Bearer {token}。
                                """)
                        .contact(new Contact().name("Gourmet Recommend").email("dev@gourmet.com")))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT 登录令牌")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME));
    }
}
