package com.okabe.service;

import com.okabe.dto.request.UpdateProfileRequest;
import com.okabe.dto.response.UserResponse;
import com.okabe.security.UserPrincipal;

public interface UserService {
    // Lấy thông tin user hiện tại
    UserResponse getCurrentUser(UserPrincipal currentUser);
    // Cập nhật thông tin hồ sơ user
    UserResponse updateProfile(UpdateProfileRequest request, UserPrincipal currentUser);
    // Upload ảnh đại diện
    UserResponse uploadAvatar(org.springframework.web.multipart.MultipartFile file, UserPrincipal currentUser) throws java.io.IOException;
}
