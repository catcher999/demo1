package com.example.demo.controller;


import com.example.demo.service.UserServiceImp;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    //还缺少令牌
    @RequestMapping("/query")
    public ResponseEntity<UserServiceImp> query() {
        return ResponseEntity.ok(new UserServiceImp());
    }

    @RequestMapping("/Modify")
    public boolean modify() {
        return true;
    }
}
