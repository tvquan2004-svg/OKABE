package com.okabe.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateLabelRequest(
    @Size(max = 100)
    String name,
    
    @Size(max = 20)
    String color
) {}
