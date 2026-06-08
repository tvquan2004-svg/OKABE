package com.okabe.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCardRequest(
        @NotBlank(message = "Card title is required") // Tiêu đề thẻ không được để trống
        @Size(max = 500, message = "Card title must not exceed 500 characters") // Giới hạn 500 ký tự
        String title, // Tiêu đề của thẻ mới

        String description, // Mô tả thẻ (không bắt buộc)

        String priority, // Mức độ ưu tiên (LOW, MEDIUM, HIGH, CRITICAL)

        String dueDate, // Hạn chót (chuỗi ngày tháng, không bắt buộc)

        String startDate // Ngày bắt đầu (chuỗi ngày tháng, không bắt buộc)
) {}
