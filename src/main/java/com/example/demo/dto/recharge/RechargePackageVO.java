package com.example.demo.dto.recharge;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 充值套餐返回对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RechargePackageVO {

    private Long id;

    private String name;

    private Integer points;

    private BigDecimal price;

    private String type;
}
