package com.okabe.dto.response;

public record SentimentResult(
    String sentiment,
    double score,
    String reason
) {
    public boolean isNegative() {
        return "NEGATIVE".equalsIgnoreCase(sentiment) && score > 0.7;
    }
}