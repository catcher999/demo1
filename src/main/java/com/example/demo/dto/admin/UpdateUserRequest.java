package com.example.demo.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改用户信息请求（name/email/role/status 全部可选，按需传）
 */
@Data
public class UpdateUserRequest {

    @Size(max = 20, message = "用户名长度不能超过 20")
    private String name;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过 100")
    private String email;

    @Size(max = 20, message = "角色长度不能超过 20")
    private String role;

    /** 1=正常 0=禁用；不传则不修改 */
    private Integer status;
}
