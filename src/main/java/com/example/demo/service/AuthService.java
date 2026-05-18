package com.example.demo.service;

import com.example.demo.entity.User;

public interface AuthService {
    /**
     * 验证用户名和密码
     * @param name 用户名
     * @param password 密码
     * @return 验证成功返回用户对象，失败返回null
     */
    User authenticate(String name, String password);
    /**
     * 登出
     */
    //void logout();
    /**
     * 注册
     * @param user 用户对象
     * @return 注册成功返回true，失败返回false
     */
    boolean register(User user);

}

