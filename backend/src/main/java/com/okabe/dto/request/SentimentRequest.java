package com.okabe.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SentimentRequest(
    @NotBlank @Size(max = 2000) String text
) {}