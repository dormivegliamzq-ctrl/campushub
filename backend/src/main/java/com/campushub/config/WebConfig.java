package com.campushub.config;

import com.campushub.interceptor.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置：注册 JWT 拦截器
 * 拦截范围：/api/** 全部接口
 * 白名单：注册、登录、健康检查（无需登录）
 * 其余接口的登录要求由 @RequireLogin 注解逐个控制（见 JwtInterceptor）
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/**",   // 注册/登录
                        "/api/health"     // 健康检查
                );
    }
}
