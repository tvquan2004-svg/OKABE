package com.okabe.service.impl;

import com.okabe.dto.request.UpdateProfileRequest;
import com.okabe.dto.response.UserResponse;
import com.okabe.entity.User;
import com.okabe.exception.ResourceNotFoundException;
import com.okabe.repository.UserRepository;
import com.okabe.security.UserPrincipal;
import com.okabe.service.StorageService;
import com.okabe.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final StorageService storageService;

    @Override
    public UserResponse getCurrentUser(UserPrincipal currentUser) {
        User user = userRepository.findById(currentUser.getId()) // Tìm người dùng theo ID
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUser.getId())); // Ném lỗi nếu không tìm thấy
        return toResponse(user); // Chuyển đổi và trả về UserResponse
    }

    @Override
    @Transactional
    public UserResponse updateProfile(UpdateProfileRequest request, UserPrincipal currentUser) {
        User user = userRepository.findById(currentUser.getId()) // Tìm người dùng
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUser.getId())); // Ném lỗi nếu không tìm thấy
        
        user.setUsername(request.getUsername()); // Cập nhật tên người dùng
        if (request.getAvatarUrl() != null) { // Nếu có URL avatar mới
            user.setAvatarUrl(request.getAvatarUrl()); // Cập nhật avatar
        }
        
        user = userRepository.save(user); // Lưu thay đổi
        return toResponse(user); // Trả về phản hồi
    }

    @Override
    @Transactional
    public UserResponse uploadAvatar(MultipartFile file, UserPrincipal currentUser) throws IOException {
        User user = userRepository.findById(currentUser.getId()) // Tìm người dùng
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUser.getId())); // Ném lỗi nếu không tìm thấy
        
        String avatarUrl = storageService.upload(file); // Tải ảnh lên storage
        
        // Delete old avatar if it was a cloudinary URL
        if (user.getAvatarUrl() != null && user.getAvatarUrl().contains("cloudinary")) { // Nếu có avatar cũ trên cloudinary
            storageService.delete(user.getAvatarUrl()); // Xóa avatar cũ
        }
        
        user.setAvatarUrl(avatarUrl); // Cập nhật avatar mới
        user = userRepository.save(user); // Lưu thay đổi
        return toResponse(user); // Trả về phản hồi
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId()) // Gán ID người dùng
                .username(user.getUsername()) // Gán tên người dùng
                .email(user.getEmail()) // Gán email
                .avatarUrl(user.getAvatarUrl()) // Gán URL ảnh đại diện
                .is2faEnabled(user.getIs2faEnabled()) // Gán trạng thái 2FA
                .build(); // Xây dựng UserResponse
    }
}
