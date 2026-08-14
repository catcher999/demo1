package com.example.demo.service.user;

import com.example.demo.dto.user.UpdateProfileRequest;
import com.example.demo.dto.user.UserVO;

/**
 * 用户服务
 * 负责：获取用户信息、修改个人信息
 */
public interface UserService {

    /**
     * 获取当前用户信息
     */
    UserVO getProfile(Long userId);

    /**
     * 修改个人信息（用户名）
     */
    UserVO updateProfile(Long userId, UpdateProfileRequest request);

    /**
     * 修改密码（验证码登录的账号首次设置密码，或已有密码时修改）
     * @param oldPassword 旧密码（首次设置时传 null）
     * @param newPassword 新密码
     */
    void updatePassword(Long userId, String oldPassword, String newPassword);
}
