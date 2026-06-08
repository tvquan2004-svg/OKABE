package com.okabe.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReorderBoardRequest(
        @NotNull(message = "Ordered board IDs are required") // Danh sách ID không được null
        List<Long> orderedIds // Danh sách ID bảng theo thứ tự mới
) {}
