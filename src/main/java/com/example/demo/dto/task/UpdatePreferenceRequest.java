package com.example.demo.dto.task;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePreferenceRequest {
    @Size(max = 200, message = "偏好不能超过 200 字")
    private String preference;
}
