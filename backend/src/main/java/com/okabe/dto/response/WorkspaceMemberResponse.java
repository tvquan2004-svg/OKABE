package com.okabe.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class WorkspaceMemberResponse {
    private Long userId;
    private String username;
    private String email;
    private String avatarUrl;
    private String role;
    private LocalDateTime joinedAt;
}
