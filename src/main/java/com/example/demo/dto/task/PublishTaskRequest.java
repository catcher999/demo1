package com.example.demo.dto.task;

import lombok.Data;

@Data
public class PublishTaskRequest {
    private String title;
    private Long categoryId;
    private Boolean isPublic;
}
