package com.okabe.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ObjectiveRequest(
        @NotBlank(message = "Title is required") // Tiêu đề không được để trống
        @Size(max = 500, message = "Title must not exceed 500 characters") // Giới hạn 500 ký tự
        String title, // Tiêu đề của mục tiêu

        String description, // Mô tả mục tiêu (không bắt buộc)

        @NotBlank(message = "Quarter is required") // Quý không được để trống
        String quarter // Quý thực hiện (VD: "2025-Q1", "2025-Q2")
) {}
