package com.okabe.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Username is required") // Tên người dùng không được để trống
        @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters") // Độ dài 3-100 ký tự
        String username, // Tên người dùng muốn đăng ký

        @NotBlank(message = "Email is required") // Email không được để trống
        @Email(message = "Invalid email format") // Kiểm tra định dạng email
        String email, // Địa chỉ email đăng ký

        @NotBlank(message = "Password is required") // Mật khẩu không được để trống
        @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters") // Độ dài 6-100 ký tự
        String password // Mật khẩu đăng ký
) {}
