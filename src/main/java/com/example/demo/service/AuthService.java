package com.example.demo.service;

import com.example.demo.dto.response.LoginResponse;

public interface AuthService {

    /**
     * 邮箱 + 密码登录
     * @param email    邮箱
     * @param password 明文密码
     * @return LoginResponse
     */
    LoginResponse login(String email, String password);

    /**
     * 发送邮箱验证码（5 分钟有效）
     * @param email 收件邮箱
     */
    void sendCode(String email);

    /**
     * 邮箱 + 验证码登录；若用户不存在则自动注册（name 留空待用户设置）
     * @param email 邮箱
     * @param code  验证码
     * @return LoginResponse
     */
    LoginResponse emailLogin(String email, String code);

    /**
     * 登出（暂无黑名单机制）
     */
    void logout();
}
