package com.okabe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private Long id;
    private Long cardId;
    private UserResponse author;
    private String content;
    private Boolean isEdited;
    private Set<UserResponse> mentions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
