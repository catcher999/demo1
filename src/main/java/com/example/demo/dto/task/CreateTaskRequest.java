package com.example.demo.dto.task;

import lombok.Data;

@Data
public class CreateTaskRequest {
    private Long sessionId;
    private String prompt;
}
