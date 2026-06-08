package com.okabe.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateListRequest(
        @NotBlank(message = "List name is required") // Tên danh sách không được để trống
        @Size(max = 255, message = "List name must not exceed 255 characters") // Giới hạn 255 ký tự
        String name // Tên của danh sách mới
) {}
