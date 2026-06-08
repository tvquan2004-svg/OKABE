package com.okabe.dto.response;

import lombok.Builder;
import java.time.LocalDateTime;

@Builder // Hỗ trợ builder pattern
public record AttachmentResponse(
    Long id, // ID của tệp đính kèm
    Long cardId, // ID thẻ chứa tệp
    Long uploadedById, // ID người tải tệp lên
    String uploadedByUsername, // Tên người tải tệp lên
    String filename, // Tên tệp gốc
    String url, // Đường dẫn truy cập tệp
    Long fileSize, // Kích thước tệp (byte)
    String mimeType, // Kiểu MIME của tệp
    LocalDateTime createdAt // Thời gian tải lên
) {}
