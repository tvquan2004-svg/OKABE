package com.okabe.controller;

import com.okabe.dto.request.UpdateProfileRequest;
import com.okabe.dto.response.ApiResponse;
import com.okabe.dto.response.UserResponse;
import com.okabe.security.UserPrincipal;
import com.okabe.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "User management APIs")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(userService.getCurrentUser(currentUser))); // Lấy thông tin hồ sơ user hiện tại
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(userService.updateProfile(request, currentUser))); // Cập nhật hồ sơ user
    }

    @PostMapping("/avatar")
    @Operation(summary = "Upload user avatar")
    public ResponseEntity<ApiResponse<UserResponse>> uploadAvatar(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @AuthenticationPrincipal UserPrincipal currentUser) throws java.io.IOException {
        return ResponseEntity.ok(ApiResponse.success(userService.uploadAvatar(file, currentUser))); // Upload ảnh đại diện
    }
}
