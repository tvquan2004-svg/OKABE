package com.okabe.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CommandRequest(
    @NotBlank String command // Câu lệnh văn bản cần xử lý (không được để trống)
) {}
