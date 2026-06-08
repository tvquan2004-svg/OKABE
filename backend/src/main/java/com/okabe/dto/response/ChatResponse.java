package com.okabe.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder // Hỗ trợ builder pattern
@JsonInclude(JsonInclude.Include.NON_NULL) // Không bao gồm trường null trong JSON
public class ChatResponse {
    private Long conversationId; // ID cuộc trò chuyện
    private Long messageId; // ID tin nhắn phản hồi
    private String reply; // Nội dung trả lời từ AI
    private LocalDateTime createdAt; // Thời gian phản hồi
}
