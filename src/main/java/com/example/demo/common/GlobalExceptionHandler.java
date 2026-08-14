package com.example.demo.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * 全局异常处理器
 * 所有异常统一返回 ResponseEntity.ok() + Result JSON
 * 用 Result.code 区分业务结果，HTTP 状态码恒为 200
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：最常见 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return ResponseEntity.ok(
                Result.error(ResultCode.BIZ_ERROR.getCode(), e.getMessage())
        );
    }

    /** 参数校验失败：@Valid/@Validated 触发 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : ResultCode.BIZ_ERROR.getMessage();
        log.warn("参数校验失败: {}", message);
        return ResponseEntity.ok(
                Result.error(ResultCode.BIZ_ERROR.getCode(), message)
        );
    }

    /** 接口不存在 */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Result<Void>> handleNotFound(NoHandlerFoundException e) {
        log.warn("接口不存在: {}", e.getRequestURL());
        return ResponseEntity.ok(
                Result.error(ResultCode.NOT_FOUND.getCode(), ResultCode.NOT_FOUND.getMessage())
        );
    }

    /** 兜底：未知系统异常 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("系统异常", e);
        return ResponseEntity.ok(
                Result.error(ResultCode.SYS_ERROR.getCode(), ResultCode.SYS_ERROR.getMessage())
        );
    }
}
