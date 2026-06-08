package com.okabe.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder // Hỗ trợ builder pattern
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
    
    @NotBlank(message = "Username cannot be blank") // Tên người dùng không được để trống
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters") // Độ dài 3-50 ký tự
    private String username; // Tên hiển thị mới của người dùng

    private String avatarUrl; // URL ảnh đại diện mới (không bắt buộc)
}
