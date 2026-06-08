package com.okabe.dto.request;

import jakarta.validation.constraints.NotNull;

public record SuggestPriorityRequest(
    @NotNull Long cardId // ID thẻ cần gợi ý mức độ ưu tiên (không được null)
) {
}
