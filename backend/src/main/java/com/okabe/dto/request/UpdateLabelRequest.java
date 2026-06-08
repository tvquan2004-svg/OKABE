package com.okabe.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateLabelRequest(
    @Size(max = 100) // Giới hạn 100 ký tự
    String name, // Tên mới của nhãn (không bắt buộc)

    @Size(max = 20) // Giới hạn 20 ký tự
    String color // Mã màu mới (không bắt buộc)
) {}
