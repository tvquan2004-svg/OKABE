package com.okabe.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SentimentRequest(
    @NotBlank @Size(max = 2000) String text // Văn bản cần phân tích cảm xúc (bắt buộc, tối đa 2000 ký tự)
) {}