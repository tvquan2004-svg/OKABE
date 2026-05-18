package com.okabe.dto.request;

import jakarta.validation.constraints.NotNull;

public record BreakdownRequest(
    @NotNull Long cardId
) {
}
