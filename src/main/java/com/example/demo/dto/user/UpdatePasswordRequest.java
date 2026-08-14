package com.example.demo.dto.user;

import lombok.Data;

@Data
public class UpdatePasswordRequest {
    /** 旧密码（首次设置密码时传 null） */
    private String oldPassword;

    /** 新密码 */
    private String newPassword;
}
