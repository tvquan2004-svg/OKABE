package com.okabe.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateChecklistItemRequest(
    @Size(max = 500)
    String content,
    
    Boolean isCompleted,
    
    Integer position
) {}
