package com.example.demo.dto.task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务队列消息载体
 * 硬约束：必须包含 taskId / userId / description / model
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskMessage {
    private Long taskId;
    private Long userId;
    private String description;
    private String model;
}
