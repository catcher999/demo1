package com.example.demo.dto.recharge;

import lombok.Data;

/**
 * 创建充值订单请求
 */
@Data
public class CreateOrderRequest {

    /** 套餐ID */
    private Long packageId;

    /** 支付方式：alipay（第一版只支持 alipay） */
    private String payMethod;
}
