package com.okabe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder // Hỗ trợ builder pattern
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id; // ID người dùng
    private String username; // Tên người dùng
    private String email; // Email
    private String avatarUrl; // Ảnh đại diện
    private Boolean is2faEnabled; // Đã bật 2FA?
}
