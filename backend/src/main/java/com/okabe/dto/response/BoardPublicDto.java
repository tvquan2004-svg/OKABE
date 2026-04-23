package com.okabe.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardPublicDto {
    private Long id;
    private String name;
    private String description;
    private String background;
    private LocalDateTime createdAt;
    private List<ListResponse> lists;
    
    // Member info without email
    private List<PublicUserResponse> members;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PublicUserResponse {
        private Long id;
        private String username;
        private String avatarUrl;
    }
}
