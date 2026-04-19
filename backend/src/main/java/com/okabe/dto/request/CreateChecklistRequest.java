package com.okabe.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateChecklistRequest(
    @NotBlank
    @Size(max = 100)
    String name
) {}
