package com.example.demo.dto.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 修改充值套餐请求（全部字段可选，按需传）
 */
@Data
public class UpdatePackageRequest {

    @Size(max = 50, message = "套餐名称长度不能超过 50")
    private String name;

    @Min(value = 1, message = "算力点数必须大于 0")
    private Integer points;

    @DecimalMin(value = "0.01", message = "价格必须大于 0")
    private BigDecimal price;

    /** 套餐类型：once / monthly */
    private String type;
}
