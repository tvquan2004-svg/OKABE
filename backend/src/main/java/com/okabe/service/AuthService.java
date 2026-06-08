package com.okabe.service;

import com.okabe.dto.request.LoginRequest;
import com.okabe.dto.request.RegisterRequest;
import com.okabe.dto.response.AuthResponse;
import com.okabe.security.UserPrincipal;

public interface AuthService {

    // Đăng ký tài khoản người dùng mới
    AuthResponse register(RegisterRequest request);

    // Đăng nhập bằng email và mật khẩu
    AuthResponse login(LoginRequest request);

    // Đăng nhập bằng Google ID token
    AuthResponse googleLogin(com.okabe.dto.request.GoogleLoginRequest request);

    // Tạo access token mới từ refresh token
    AuthResponse refreshToken(String refreshToken);

    // Lấy thông tin user hiện tại
    AuthResponse.UserInfo getCurrentUser(UserPrincipal currentUser);

    // Xác thực email của user bằng token
    void verifyEmail(String token);
}
