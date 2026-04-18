package com.okabe.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCardRequest(
        @NotBlank(message = "Card title is required")
        @Size(max = 500, message = "Card title must not exceed 500 characters")
        String title,

        String description,

        String priority,

        String dueDate
) {}
