package com.okabe.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Email is required") // Email không được để trống
        @Email(message = "Invalid email format") // Kiểm tra định dạng email
        String email, // Địa chỉ email đăng nhập

        @NotBlank(message = "Password is required") // Mật khẩu không được để trống
        String password // Mật khẩu đăng nhập
) {}
