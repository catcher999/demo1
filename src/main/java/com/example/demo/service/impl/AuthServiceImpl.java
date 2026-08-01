package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.common.JwtUtil;
import com.example.demo.entity.User;
import com.example.demo.mapper.UserMapper;
import com.example.demo.service.AuthService;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    public AuthServiceImpl(UserMapper userMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public String login(String username, String password) {
        try {
            // 1. 构建查询条件：where username = ?
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getName, username);

            // 2. 执行查询（BaseMapper 自带的 selectOne）
            User user = userMapper.selectOne(wrapper);

            if (user == null) {
                throw new RuntimeException("User not found");
            }
            if (!user.getPassword().equals(password)) {
                throw new RuntimeException("Password error");
            }
            return jwtUtil.generateToken(username, "user", user.getId());
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }
}
