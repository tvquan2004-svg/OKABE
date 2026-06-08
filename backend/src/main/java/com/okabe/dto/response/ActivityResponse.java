package com.okabe.dto.response;

import lombok.Builder;
import java.time.LocalDateTime;

@Builder // Hỗ trợ builder pattern
public record ActivityResponse(
    Long id, // ID của hoạt động
    Long userId, // ID người dùng thực hiện hành động
    String username, // Tên người dùng thực hiện hành động
    String avatarUrl, // URL ảnh đại diện người dùng
    String actionType, // Loại hành động (VD: create_card, move_card)
    String description, // Mô tả chi tiết hoạt động
    Long cardId, // ID thẻ liên quan
    LocalDateTime createdAt // Thời điểm diễn ra hoạt động
) {}
