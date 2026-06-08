package com.okabe.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder // Hỗ trợ builder pattern
@JsonInclude(JsonInclude.Include.NON_NULL) // Không bao gồm trường null trong JSON
public class ConversationResponse {
    private Long id; // ID cuộc trò chuyện
    private String title; // Tiêu đề cuộc trò chuyện
    private Long boardId; // ID bảng liên quan
    private Long workspaceId; // ID workspace liên quan
    private LocalDateTime createdAt; // Thời gian tạo
    private LocalDateTime updatedAt; // Thời gian cập nhật
    private List<MessageResponse> messages; // Danh sách tin nhắn
}
