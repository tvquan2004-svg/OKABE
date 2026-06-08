package com.okabe.dto.response;

public record SentimentResult(
    String sentiment, // Kết quả cảm xúc (POSITIVE, NEGATIVE, NEUTRAL)
    double score, // Điểm tin cậy (0.0 - 1.0)
    String reason // Lý do phân tích
) {
    public boolean isNegative() { // Kiểm tra có phải cảm xúc tiêu cực mạnh không
        return "NEGATIVE".equalsIgnoreCase(sentiment) && score > 0.7;
    }
}