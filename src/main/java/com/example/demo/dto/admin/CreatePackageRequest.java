package com.example.demo.dto.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 添加充值套餐请求
 * 添加不等于上架：创建时 status 默认为 0（下架），需调"上架"接口才生效
 */
@Data
public class CreatePackageRequest {

    @NotBlank(message = "套餐名称不能为空")
    @Size(max = 50, message = "套餐名称长度不能超过 50")
    private String name;

    @NotNull(message = "算力点数不能为空")
    @jakarta.validation.constraints.Min(value = 1, message = "算力点数必须大于 0")
    private Integer points;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于 0")
    private BigDecimal price;

    /** 套餐类型：once 一次性 / monthly 月卡；不传默认 once */
    private String type;
}
