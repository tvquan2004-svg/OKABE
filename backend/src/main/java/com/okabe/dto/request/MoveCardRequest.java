package com.okabe.dto.request;

import jakarta.validation.constraints.NotNull;

public record MoveCardRequest(
    @NotNull(message = "Target list ID is required") // ID danh sách đích không được null
    Long targetListId, // ID danh sách muốn di chuyển thẻ đến

    Integer position // Vị trí mới trong danh sách đích (không bắt buộc)
) {}
