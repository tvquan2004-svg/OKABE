package com.okabe.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.okabe.entity.AiMessage.MessageRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder // Hỗ trợ builder pattern
@JsonInclude(JsonInclude.Include.NON_NULL) // Không bao gồm trường null trong JSON
public class MessageResponse {
    private Long id; // ID tin nhắn
    private MessageRole role; // Vai trò người gửi (USER, ASSISTANT, SYSTEM)
    private String content; // Nội dung tin nhắn
    private LocalDateTime createdAt; // Thời gian gửi
}
