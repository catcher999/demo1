package com.example.demo.service;

public interface AuthService {

    String login(String username, String password);
    /*
    用于将用户名和密码进行验证，并返回token
    * @param username
    * @param password
    * return token
     */
    String register(String username, String password);
    /*
    用于将用户名和密码进行注册，并返回token
    * @param username
    * @param password
    * return token
     */
    String logout();
    /*
    用于登出用户，可选记录黑名单
    * @return
     */
}
