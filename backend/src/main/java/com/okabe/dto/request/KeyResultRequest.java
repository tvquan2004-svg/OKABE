package com.okabe.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record KeyResultRequest(
        @NotBlank(message = "Title is required") // Tiêu đề không được để trống
        @Size(max = 500, message = "Title must not exceed 500 characters") // Giới hạn 500 ký tự
        String title, // Tiêu đề của kết quả then chốt

        Double targetValue, // Giá trị mục tiêu cần đạt

        String unit // Đơn vị đo (percent, number, currency...)
) {}
