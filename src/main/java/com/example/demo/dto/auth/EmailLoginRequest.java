package com.example.demo.dto.auth;

import lombok.Data;

@Data
public class EmailLoginRequest {
    private String email;
    private String code;
}
