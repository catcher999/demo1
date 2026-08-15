package com.example.demo.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 添加 AI 模型请求
 */
@Data
public class CreateModelRequest {

    @NotBlank(message = "模型标识不能为空")
    @Size(max = 50, message = "模型标识长度不能超过 50")
    private String name;

    @NotBlank(message = "显示名称不能为空")
    @Size(max = 100, message = "显示名称长度不能超过 100")
    private String displayName;

    @NotNull(message = "单次消耗算力不能为空")
    @Min(value = 0, message = "算力消耗不能为负数")
    private Integer pointsCost;
}
