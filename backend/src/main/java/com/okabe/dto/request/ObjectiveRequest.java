package com.okabe.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ObjectiveRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 500, message = "Title must not exceed 500 characters")
        String title,

        String description,

        @NotBlank(message = "Quarter is required")
        String quarter
) {}
