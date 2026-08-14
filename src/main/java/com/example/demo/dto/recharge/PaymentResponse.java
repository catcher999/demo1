package com.example.demo.dto.recharge;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支付响应：返回订单号 + 支付页面 URL（或表单 HTML）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    /** 订单号 */
    private String orderNo;

    /** 支付页面内容（沙箱版返回的是一段自动提交的 form HTML） */
    private String payUrl;
}
