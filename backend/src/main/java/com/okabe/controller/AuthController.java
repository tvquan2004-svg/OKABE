package com.okabe.controller;

import com.okabe.dto.request.LoginRequest;
import com.okabe.dto.request.RegisterRequest;
import com.okabe.dto.response.ApiResponse;
import com.okabe.dto.response.AuthResponse;
import com.okabe.security.UserPrincipal;
import com.okabe.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication & registration APIs")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request); // Gọi service đăng ký người dùng mới
        return ResponseEntity
                .status(HttpStatus.CREATED) // Trả về HTTP 201 Created
                .body(ApiResponse.success(response, "Registration successful")); // Bọc vào ApiResponse thành công
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request); // Gọi service đăng nhập
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful")); // Trả về token + user info
    }

    @PostMapping("/google")
    @Operation(summary = "Login with Google ID token")
    public ResponseEntity<ApiResponse<AuthResponse>> googleLogin(
            @Valid @RequestBody com.okabe.dto.request.GoogleLoginRequest request) {
        AuthResponse response = authService.googleLogin(request); // Xác thực Google ID token
        return ResponseEntity.ok(ApiResponse.success(response, "Google Login successful"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @RequestBody java.util.Map<String, String> request) {
        String refreshToken = request.get("refreshToken"); // Lấy refresh token từ request body
        AuthResponse response = authService.refreshToken(refreshToken); // Cấp lại access token mới
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user info")
    public ResponseEntity<ApiResponse<AuthResponse.UserInfo>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        AuthResponse.UserInfo userInfo = authService.getCurrentUser(currentUser); // Lấy thông tin user hiện tại
        return ResponseEntity.ok(ApiResponse.success(userInfo));
    }

    @GetMapping("/verify-email")
    @Operation(summary = "Verify user email using token")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @RequestParam String token) {
        authService.verifyEmail(token); // Kích hoạt tài khoản bằng token xác thực email
        return ResponseEntity.ok(ApiResponse.success(null, "Email verified successfully. You can now login."));
    }
}
