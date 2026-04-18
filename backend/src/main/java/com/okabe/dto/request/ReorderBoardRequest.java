package com.okabe.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReorderBoardRequest(
        @NotNull(message = "Ordered board IDs are required")
        List<Long> orderedIds
) {}
