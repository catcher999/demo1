package com.example.demo.dto.recharge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建充值订单请求
 */
@Data
public class CreateOrderRequest {

    @NotNull(message = "套餐ID不能为空")
    private Long packageId;

    @NotBlank(message = "支付方式不能为空")
    private String payMethod;
}
