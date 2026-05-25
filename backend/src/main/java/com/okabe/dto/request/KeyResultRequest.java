package com.okabe.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record KeyResultRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 500, message = "Title must not exceed 500 characters")
        String title,

        Double targetValue,

        String unit
) {}
