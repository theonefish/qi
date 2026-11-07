package com.example.qi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 全局配置：后台前端跨域 + 本地媒体文件静态映射。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.storage-dir}")
    private String storageDir;

    /** 后台 Vue 前端开发服务器跨域（10.2）；上线前按实际域名收紧 */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    /** 本地磁盘存储映射为 /files/** 访问地址（9.1 部署与存储口径） */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + storageDir);
    }
}
