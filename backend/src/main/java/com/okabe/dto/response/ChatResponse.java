package com.okabe.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatResponse {
    private Long conversationId;
    private Long messageId;
    private String reply;
    private LocalDateTime createdAt;
}
