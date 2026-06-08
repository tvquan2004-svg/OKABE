package com.okabe.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateChecklistItemRequest(
    @Size(max = 500) // Giới hạn 500 ký tự
    String content, // Nội dung mới của mục (không bắt buộc)

    Boolean isCompleted, // Trạng thái hoàn thành mới (không bắt buộc)

    Integer position // Vị trí mới trong checklist (không bắt buộc)
) {}
