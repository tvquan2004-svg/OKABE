package com.okabe.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateChecklistRequest(
    @NotBlank // Tên checklist không được để trống
    @Size(max = 100) // Giới hạn 100 ký tự
    String name // Tên của checklist mới
) {}
