package com.example.demo.dto.task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskVO {

    private Long id;

    private Long sessionId;

    private String prompt;

    private String description;

    private String status;

    private String resultUrl;

    private String errorMessage;

    private Long artworkId;

    private Date createdAt;

    private Date updatedAt;
}
