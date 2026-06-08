package com.okabe.dto.request;

import jakarta.validation.constraints.NotNull;

public record BreakdownRequest(
    @NotNull Long cardId // ID thẻ cần phân rã thành các tác vụ nhỏ
) {
}
