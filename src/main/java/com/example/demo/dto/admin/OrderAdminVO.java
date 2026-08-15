package com.example.demo.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 管理端订单视图
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderAdminVO {
    private Long id;
    private String orderNo;
    private Long userId;
    private Long packageId;
    private String packageName;
    private Integer points;
    private BigDecimal amount;
    private String payMethod;
    private String tradeNo;
    private String status;
    private Date createdAt;
    private Date updatedAt;
}
