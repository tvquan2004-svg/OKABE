package com.okabe.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        String idToken,
        String accessToken,
        String username
) {}
