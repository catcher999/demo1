package com.example.demo.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 管理端权限拦截器
 * 仅拦截 /api/admin/**，校验当前用户角色是否为 admin。
 * JwtInterceptor 已先于本拦截器执行，currentRole 由其注入。
 */
@Component
@Slf4j
public class AdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // JwtInterceptor 已校验 token 并注入 currentRole
        String role = (String) request.getAttribute("currentRole");
        if (!"admin".equals(role)) {
            log.warn("非管理员访问管理端被拒：role={}, path={}", role, request.getRequestURI());
            throw new BusinessException("无权限访问管理端");
        }
        return true;
    }
}
