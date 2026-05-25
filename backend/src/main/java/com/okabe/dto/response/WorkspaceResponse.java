package com.okabe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private OwnerInfo owner;
    private String currentUserRole;
    private int memberCount;
    private long boardCount;
    private LocalDateTime createdAt;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OwnerInfo {
        private Long id;
        private String username;
        private String email;
        private String avatarUrl;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberInfo {
        private Long userId;
        private String username;
        private String email;
        private String avatarUrl;
        private String role;
        private LocalDateTime joinedAt;
    }
}
