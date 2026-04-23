package com.okabe.service;

import com.okabe.dto.request.UpdateProfileRequest;
import com.okabe.dto.response.UserResponse;
import com.okabe.security.UserPrincipal;

public interface UserService {
    UserResponse getCurrentUser(UserPrincipal currentUser);
    UserResponse updateProfile(UpdateProfileRequest request, UserPrincipal currentUser);
    UserResponse uploadAvatar(org.springframework.web.multipart.MultipartFile file, UserPrincipal currentUser) throws java.io.IOException;
}
