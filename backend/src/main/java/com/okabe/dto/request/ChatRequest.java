package com.okabe.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank(message = "Tin nhắn không được để trống") // Nội dung tin nhắn không được để trống
        @Size(max = 2000, message = "Tin nhắn không được vượt quá 2000 ký tự") // Giới hạn độ dài tin nhắn
        String message, // Nội dung tin nhắn gửi đến AI

        Long conversationId, // ID cuộc trò chuyện (null nếu là cuộc trò chuyện mới)

        Long boardId, // ID bảng liên quan (để lấy ngữ cảnh)

        Long workspaceId // ID không gian làm việc liên quan (để lấy ngữ cảnh)
) {}
