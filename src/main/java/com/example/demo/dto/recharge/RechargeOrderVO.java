package com.example.demo.dto.recharge;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 充值订单返回对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RechargeOrderVO {

    private Long id;

    private String orderNo;

    private Long packageId;

    private String packageName;

    private Integer points;

    private BigDecimal amount;

    private String payMethod;

    private String status;

    private Date createdAt;

    private Date paidAt;
}
