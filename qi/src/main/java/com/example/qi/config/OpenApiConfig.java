package com.example.qi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 接口文档配置（12.3 交付物）：
 * 注册 Authorization 请求头为全局安全方案，Swagger 页面 Authorize 后
 * 所有请求自动携带 Sa-Token 令牌。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("蜀漆云馆后端 API")
                        .description("/admin/** 为后台接口需登录；/app/** 为小程序匿名接口")
                        .version("v0.1"))
                .addSecurityItem(new SecurityRequirement().addList("Authorization"))
                .components(new Components().addSecuritySchemes("Authorization",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")));
    }
}
