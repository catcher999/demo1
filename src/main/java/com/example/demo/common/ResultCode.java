package com.example.demo.common;

import lombok.Getter;

/**
 * 统一返回码枚举
 * 用 Result.code 区分业务结果，HTTP 状态码恒为 200
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "success"),
    BIZ_ERROR(400, "业务异常"),
    NOT_FOUND(404, "资源不存在"),
    SYS_ERROR(500, "系统异常");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
