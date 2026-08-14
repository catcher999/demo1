package com.example.demo.common;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
public class JwtInterceptor implements HandlerInterceptor {
    /*
     * JWT拦截器
     */
    private final JwtUtil jwtUtil;
    public JwtInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler){
        // 1. 从 Header 获取 Token
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException("缺少有效的授权令牌");
        }
        String token = authHeader.substring(7);

        // 2. 验证 Token 并解析 Claims（一次解析，避免重复 3 次）
        Claims claims = jwtUtil.validateAndParseToken(token);
        if (claims == null) {
            throw new BusinessException("Token 无效或已过期，请重新登录");
        }

        // 3. 提取用户信息（从已解析的 Claims 取，无需再解析）
        Long userId = (Long) claims.get("userId");
        String username = claims.getSubject();

        // 4. 存入 Request 上下文
        request.setAttribute("currentUserId", userId);
        request.setAttribute("currentUsername", username);

        log.debug("Token 验证通过，用户ID: {}, 用户名: {}", userId, username);
        return true;
    }
}
