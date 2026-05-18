package com.okabe.dto.response;

public record PrioritySuggestion(
    String suggestedPriority,
    int score,
    String reason
) {
}
