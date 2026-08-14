package com.example.demo.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.demo.common.BusinessException;
import com.example.demo.dto.user.UpdateProfileRequest;
import com.example.demo.dto.user.UserVO;
import com.example.demo.entity.user.User;
import com.example.demo.mapper.user.UserMapper;
import com.example.demo.service.user.UserService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserVO getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return toVO(user);
    }

    @Override
    public UserVO updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (request.getName() == null || request.getName().isBlank()) {
            throw new BusinessException("用户名不能为空");
        }
        if (request.getName().length() > 20) {
            throw new BusinessException("用户名长度不能超过 20");
        }

        userMapper.update(null,
                new LambdaUpdateWrapper<User>()
                        .eq(User::getId, userId)
                        .set(User::getName, request.getName()));

        user.setName(request.getName());
        return toVO(user);
    }

    @Override
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException("新密码长度至少 6 位");
        }

        // 已有密码时，必须校验旧密码
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            if (oldPassword == null || !passwordEncoder.matches(oldPassword, user.getPassword())) {
                throw new BusinessException("旧密码错误");
            }
        }

        // 加密新密码
        String encoded = passwordEncoder.encode(newPassword);
        userMapper.update(null,
                new LambdaUpdateWrapper<User>()
                        .eq(User::getId, userId)
                        .set(User::getPassword, encoded));
    }

    private UserVO toVO(User user) {
        return new UserVO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getPoints(),
                user.getSignDate(),
                user.getSignStreak()
        );
    }
}
