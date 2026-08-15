package com.example.demo.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改 AI 模型请求（全部字段可选，按需传；含算力单价调整）
 */
@Data
public class UpdateModelRequest {

    @Size(max = 50, message = "模型标识长度不能超过 50")
    private String name;

    @Size(max = 100, message = "显示名称长度不能超过 100")
    private String displayName;

    @Min(value = 0, message = "算力消耗不能为负数")
    private Integer pointsCost;
}
