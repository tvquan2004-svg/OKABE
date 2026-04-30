package com.okabe.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.okabe.entity.AiMessage.MessageRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageResponse {
    private Long id;
    private MessageRole role;
    private String content;
    private LocalDateTime createdAt;
}
