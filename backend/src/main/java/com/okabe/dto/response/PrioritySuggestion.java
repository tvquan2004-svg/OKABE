package com.okabe.dto.response;

public record PrioritySuggestion(
    String suggestedPriority, // Mức ưu tiên được gợi ý (LOW, MEDIUM, HIGH, CRITICAL)
    int score, // Điểm tin cậy của gợi ý
    String reason // Lý do cho gợi ý
) {
}
