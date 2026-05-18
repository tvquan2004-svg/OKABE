package com.okabe.dto.request;

import jakarta.validation.constraints.NotNull;

public record SuggestPriorityRequest(
    @NotNull Long cardId
) {
}
