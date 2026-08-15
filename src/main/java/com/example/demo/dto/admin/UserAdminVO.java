package com.example.demo.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 管理端用户视图（比普通 UserVO 多 status 字段）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAdminVO {
    private Long id;
    private String name;
    private String email;
    private String role;
    private Integer points;
    private Integer status;
    private LocalDate signDate;
    private Integer signStreak;
}
