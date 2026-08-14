package com.example.demo.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {
    private Long id;
    private String name;
    private String email;
    private String role;
    private Integer points;
    private LocalDate signDate;
    private Integer signStreak;
}
