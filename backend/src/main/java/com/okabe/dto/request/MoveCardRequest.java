package com.okabe.dto.request;

import jakarta.validation.constraints.NotNull;

public record MoveCardRequest(
    @NotNull(message = "Target list ID is required")
    Long targetListId,
    
    Integer position
) {}
