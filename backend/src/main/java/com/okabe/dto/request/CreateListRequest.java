package com.okabe.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateListRequest(
        @NotBlank(message = "List name is required")
        @Size(max = 255, message = "List name must not exceed 255 characters")
        String name
) {}
