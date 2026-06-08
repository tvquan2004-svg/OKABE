package com.okabe.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateLabelRequest(
    @Size(max = 100) // Giới hạn 100 ký tự
    String name, // Tên của nhãn mới

    @NotBlank // Màu sắc không được để trống
    @Size(max = 20) // Giới hạn 20 ký tự
    String color // Mã màu (VD: "#FF0000", "red")
) {}
