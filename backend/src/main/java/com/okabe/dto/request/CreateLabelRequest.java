package com.okabe.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateLabelRequest(
    @Size(max = 100)
    String name,
    
    @NotBlank
    @Size(max = 20)
    String color
) {}
