package com.okabe.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CommandRequest(
    @NotBlank String command
) {}
