package com.okabe.dto.response;

import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record AttachmentResponse(
    Long id,
    Long cardId,
    Long uploadedById,
    String uploadedByUsername,
    String filename,
    String url,
    Long fileSize,
    String mimeType,
    LocalDateTime createdAt
) {}
