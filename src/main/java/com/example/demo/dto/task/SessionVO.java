package com.example.demo.dto.task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionVO {

    private Long id;

    private String title;

    private String preference;

    private Date createdAt;

    private Date updatedAt;
}
