package com.okabe.dto.response;

import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record NotificationResponse(
    Long id,
    Long actorId,
    String actorName,
    String actorAvatarUrl,
    String type,
    String entityType,
    Long entityId,
    String message,
    Boolean isRead,
    LocalDateTime createdAt
) {
}
