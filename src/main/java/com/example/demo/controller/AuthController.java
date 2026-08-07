package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.dto.request.EmailLoginRequest;
import com.example.demo.dto.request.LoginRequest;
import com.example.demo.dto.request.SendCodeRequest;
import com.example.demo.dto.response.LoginResponse;
import com.example.demo.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 邮箱 + 密码登录 */
    @PostMapping("/login")
    public ResponseEntity<Result<LoginResponse>> login(@RequestBody LoginRequest req) {
        LoginResponse data = authService.login(req.getEmail(), req.getPassword());
        return ResponseEntity.ok(Result.success("Login successful", data));
    }

    /** 发送邮箱验证码（5 分钟有效） */
    @PostMapping("/send-code")
    public ResponseEntity<Result<Void>> sendCode(@RequestBody SendCodeRequest req) {
        authService.sendCode(req.getEmail());
        return ResponseEntity.ok(Result.success("Verification code sent", null));
    }

    /** 邮箱 + 验证码登录；用户不存在则自动注册 */
    @PostMapping("/email-login")
    public ResponseEntity<Result<LoginResponse>> emailLogin(@RequestBody EmailLoginRequest req) {
        LoginResponse data = authService.emailLogin(req.getEmail(), req.getCode());
        return ResponseEntity.ok(Result.success("Login successful", data));
    }

    /** 登出（暂无黑名单机制） */
    @PostMapping("/logout")
    public ResponseEntity<Result<Void>> logout() {
        authService.logout();
        return ResponseEntity.ok(Result.success("Logout successful", null));
    }
}
