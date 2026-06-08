package com.okabe.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateChecklistItemRequest(
    @NotBlank // Nội dung mục không được để trống
    @Size(max = 500) // Giới hạn 500 ký tự
    String content // Nội dung của mục checklist mới
) {}
