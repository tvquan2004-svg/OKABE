package com.okabe.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBoardRequest(
        @NotBlank(message = "Board name is required")
        @Size(max = 255, message = "Board name must not exceed 255 characters")
        String name,

        String description,

        String background
) {}
