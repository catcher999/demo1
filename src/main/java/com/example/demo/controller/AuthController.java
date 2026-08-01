package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.dto.reponse.LoginResponse;
import com.example.demo.service.impl.AuthServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthServiceImpl authServiceImpl;

    public AuthController(AuthServiceImpl authServiceImpl) {
        this.authServiceImpl = authServiceImpl;
    }
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String,String> body){
        String username = body.get("username");
        String password = body.get("password");

        try {
            String token = authServiceImpl.login(username, password);
            // 返回token，由LoginResponse承接service返回的token
            // 返回登入信息，由Result包装LoginResponse
            // 返回总登入情况，由于确实处理了请求所以.ok()，由ResponseEntity包装Result
            LoginResponse loginResponse = new LoginResponse(token,);
            return ResponseEntity.ok(Result.success("Login successful",loginResponse));
        } catch (Exception e) {

        }
    }
    //登出没有实际逻辑，只返回成功
    @PostMapping("/logout")
    public ResponseEntity<?> logout(){
        return ResponseEntity.ok(Result.success("Logout successful"));
    }
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String,String> body){
        String username = body.get("username");
        String password = body.get("password");

        try {
            String token = authServiceImpl.register(username, password);
            // 创建用户，返回token
            // 创建用户，返回登入信息
            // 创建用户，返回总登入情况
        } catch (Exception e) {

        }
    }
}
