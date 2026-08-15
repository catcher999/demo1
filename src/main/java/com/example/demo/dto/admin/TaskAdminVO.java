package com.example.demo.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 管理端任务视图（全站任务流水）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskAdminVO {
    private Long id;
    private Long sessionId;
    private Long userId;
    private String prompt;
    private String description;
    private String status;
    private String resultUrl;
    private String errorMessage;
    private Long artworkId;
    private Integer pointsCost;
    private String model;
    private Date queuedAt;
    private Date finishedAt;
    private Date createdAt;
}
