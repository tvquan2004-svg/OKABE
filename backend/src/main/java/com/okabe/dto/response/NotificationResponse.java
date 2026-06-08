package com.okabe.dto.response;

import lombok.Builder;
import java.time.LocalDateTime;

@Builder // Hỗ trợ builder pattern
public record NotificationResponse(
    Long id, // ID thông báo
    Long actorId, // ID người thực hiện hành động
    String actorName, // Tên người thực hiện
    String actorAvatarUrl, // Ảnh đại diện người thực hiện
    String type, // Loại thông báo
    String entityType, // Loại đối tượng liên quan
    Long entityId, // ID đối tượng
    Long extraId, // ID bổ sung
    String message, // Nội dung thông báo
    Boolean isRead, // Đã đọc?
    LocalDateTime createdAt // Thời gian tạo
) {
}
