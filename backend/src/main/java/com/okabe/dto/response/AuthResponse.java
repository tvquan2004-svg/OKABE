package com.okabe.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    @JsonProperty("accessToken")
    private String accessToken;
    @JsonProperty("refreshToken")
    private String refreshToken;
    @JsonProperty("tokenType")
    private String tokenType;
    private UserInfo user;
    private boolean needsRegistration;
    private String email;
    private String avatarUrl;
    private String googleName;
    private boolean requires2fa;
    private String tempToken;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String email;
        private String username;
        private String avatarUrl;
        
        @JsonProperty("is2faEnabled")
        private boolean is2faEnabled;
    }
}
