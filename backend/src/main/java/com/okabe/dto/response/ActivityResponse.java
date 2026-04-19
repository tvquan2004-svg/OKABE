package com.okabe.dto.response;

import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record ActivityResponse(
    Long id,
    Long userId,
    String username,
    String avatarUrl,
    String actionType,
    String description,
    LocalDateTime createdAt
) {}
