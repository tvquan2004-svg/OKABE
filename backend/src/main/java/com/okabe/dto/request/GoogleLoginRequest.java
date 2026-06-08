package com.okabe.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        String idToken, // Token ID từ Google (dùng xác thực)
        String accessToken, // Access token từ Google (dùng gọi API)
        String username // Tên người dùng muốn đặt (nếu đăng ký mới)
) {}
