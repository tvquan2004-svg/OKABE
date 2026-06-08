package com.okabe.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder // Hỗ trợ builder pattern
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    @JsonProperty("accessToken") // Ánh xạ JSON: accessToken
    private String accessToken; // Token truy cập JWT
    @JsonProperty("refreshToken") // Ánh xạ JSON: refreshToken
    private String refreshToken; // Token làm mới
    @JsonProperty("tokenType") // Ánh xạ JSON: tokenType
    private String tokenType; // Loại token (VD: "Bearer")
    private UserInfo user; // Thông tin người dùng
    private boolean needsRegistration; // Cần đăng ký bổ sung không (Google login)
    private String email; // Email người dùng (cho luồng đăng ký)
    private String avatarUrl; // URL ảnh đại diện
    private String googleName; // Tên từ Google (cho luồng đăng ký)
    private boolean requires2fa; // Yêu cầu xác thực 2FA
    private String tempToken; // Token tạm thời cho xác thực 2FA

    @Getter
    @Setter
    @Builder // Hỗ trợ builder pattern
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo { // Thông tin người dùng trong phản hồi xác thực
        private Long id; // ID người dùng
        private String email; // Email người dùng
        private String username; // Tên người dùng
        private String avatarUrl; // URL ảnh đại diện

        @JsonProperty("is2faEnabled") // Ánh xạ JSON: is2faEnabled
        private boolean is2faEnabled; // Trạng thái bật 2FA
    }
}
