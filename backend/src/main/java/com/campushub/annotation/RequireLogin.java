package com.campushub.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口级登录控制注解：
 * 标了它的接口必须携带有效 token 才能访问，由 JwtInterceptor 统一校验；
 * 没标的接口游客可访问（若请求带了合法 token，拦截器也会顺便把用户信息放入 UserContext）。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireLogin {
}
