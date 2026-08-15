package com.example.demo.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 启用/禁用用户请求
 */
@Data
public class UpdateUserStatusRequest {

    @NotNull(message = "状态不能为空")
    private Integer status;

    public boolean isValidStatus() {
        return status != null && (status == 0 || status == 1);
    }
}
