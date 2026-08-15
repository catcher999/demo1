package com.example.demo.entity.admin;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管理端操作审计实体
 * 仅记录高危操作：算力调整 / 启用禁用用户 等
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作人ID（管理员） */
    private Long operatorId;

    /** 操作类型，如 ADJUST_POINTS / DISABLE_USER / ENABLE_USER / UPDATE_USER */
    private String operation;

    /** 目标类型，如 USER / POINTS */
    private String targetType;

    /** 目标ID */
    private Long targetId;

    /** 请求参数 JSON */
    private String requestParams;

    private String ip;

    /** 1=成功 0=失败 */
    private Integer status;

    private String errorMsg;

    private LocalDateTime createdAt;
}
