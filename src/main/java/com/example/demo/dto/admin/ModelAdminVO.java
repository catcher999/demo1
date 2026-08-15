package com.example.demo.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理端 AI 模型视图
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelAdminVO {
    private Long id;
    private String name;
    private String displayName;
    private Integer pointsCost;
    private Integer status;
}
