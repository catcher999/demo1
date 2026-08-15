package com.example.demo.dto.admin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员手动调整算力请求
 * delta 可正可负：正=补偿，负=扣除
 */
@Data
public class AdjustPointsRequest {

    @NotNull(message = "变动值不能为空")
    private Integer delta;

    @Size(max = 200, message = "备注长度不能超过 200")
    private String remark;
}
