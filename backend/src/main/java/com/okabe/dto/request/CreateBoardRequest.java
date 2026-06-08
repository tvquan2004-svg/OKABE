package com.okabe.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBoardRequest(
        @NotBlank(message = "Board name is required") // Tên bảng không được để trống
        @Size(max = 255, message = "Board name must not exceed 255 characters") // Giới hạn 255 ký tự
        String name, // Tên của bảng mới

        String description, // Mô tả bảng (không bắt buộc)

        String background, // Đường dẫn ảnh nền hoặc mã màu (không bắt buộc)

        Long templateId // ID mẫu bảng để tạo từ template (không bắt buộc)
) {}
