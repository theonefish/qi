package com.example.qi.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 后台接口统一登录拦截（9.4）：
 * /admin/** 必须登录后访问；/app/** 小程序接口匿名访问；未登录由全局异常处理器返回 401。
 *
 * 注意：浏览器跨域请求前会先发送不带自定义请求头的 OPTIONS 预检，
 * 因此预检请求必须放行，否则管理后台（5173）的所有接口都会因预检失败而报网络错误。
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            String method = SaHolder.getRequest().getMethod();
            if (!"OPTIONS".equalsIgnoreCase(method)) {
                StpUtil.checkLogin();
            }
        }))
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/auth/login");
    }
}
