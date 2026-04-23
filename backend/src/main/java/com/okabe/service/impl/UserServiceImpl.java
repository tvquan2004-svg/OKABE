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
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUser.getId()));
        return toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(UpdateProfileRequest request, UserPrincipal currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUser.getId()));
        
        user.setUsername(request.getUsername());
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        
        user = userRepository.save(user);
        return toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse uploadAvatar(MultipartFile file, UserPrincipal currentUser) throws IOException {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUser.getId()));
        
        String avatarUrl = storageService.upload(file);
        
        // Delete old avatar if it was a cloudinary URL
        if (user.getAvatarUrl() != null && user.getAvatarUrl().contains("cloudinary")) {
            storageService.delete(user.getAvatarUrl());
        }
        
        user.setAvatarUrl(avatarUrl);
        user = userRepository.save(user);
        return toResponse(user);
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .is2faEnabled(user.getIs2faEnabled())
                .build();
    }
}
