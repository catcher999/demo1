package com.example.demo.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 管理端套餐视图（含下架的，含状态字段）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PackageAdminVO {
    private Long id;
    private String name;
    private Integer points;
    private BigDecimal price;
    private String type;
    private Integer status;
    private Date createdAt;
    private Date updatedAt;
}
