package com.campushub.interceptor;

import com.campushub.annotation.RequireLogin;
import com.campushub.common.Result;
import com.campushub.common.ResultCode;
import com.campushub.common.UserContext;
import com.campushub.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 登录拦截器（接口级注解控制）：
 * 1. 目标方法标了 @RequireLogin → 必须携带有效 token，否则 401
 * 2. 目标方法没标（游客可访问）→ 如果带了合法 token，也把用户信息放入 UserContext
 *    （例如看帖子详情时顺便带上“我是否已点赞”的视角）
 * 3. ThreadLocal 在请求结束必须清理，防止线程池复用串号
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // CORS 预检请求（OPTIONS）不携带业务头，直接放行
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        boolean required = isLoginRequired(handler);

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            try {
                Claims claims = jwtUtil.parseToken(authHeader.substring(BEARER_PREFIX.length()));
                Long userId = Long.valueOf(claims.getSubject());
                String username = claims.get("username", String.class);
                UserContext.set(userId, username);
                return true;
            } catch (Exception e) {
                // 必须登录的接口：token 无效直接拒绝
                if (required) {
                    return reject(response, "登录已过期，请重新登录");
                }
                // 游客接口：token 无效就当游客处理，继续放行
                return true;
            }
        }

        if (required) {
            return reject(response, "未登录");
        }
        return true;
    }

    /** 判断当前请求的接口是否要求登录（标了 @RequireLogin 注解） */
    private boolean isLoginRequired(Object handler) {
        if (handler instanceof HandlerMethod handlerMethod) {
            return handlerMethod.hasMethodAnnotation(RequireLogin.class);
        }
        return false;
    }

    private boolean reject(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                Result.fail(ResultCode.UNAUTHORIZED, message)));
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // ThreadLocal 必须清理：Tomcat 线程池会复用线程，不清会导致用户信息串号
        UserContext.clear();
    }
}
