package com.okabe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder // Hỗ trợ builder pattern
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceResponse {
    private Long id; // ID workspace
    private String name; // Tên workspace
    private String slug; // Slug URL
    private String description; // Mô tả
    private OwnerInfo owner; // Thông tin chủ sở hữu
    private String currentUserRole; // Vai trò người dùng hiện tại
    private int memberCount; // Số lượng thành viên
    private long boardCount; // Số lượng bảng
    private LocalDateTime createdAt; // Thời gian tạo

    @Getter
    @Builder // Hỗ trợ builder pattern
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OwnerInfo { // Thông tin chủ sở hữu
        private Long id; // ID người dùng
        private String username; // Tên người dùng
        private String email; // Email
        private String avatarUrl; // Ảnh đại diện
    }

    @Getter
    @Builder // Hỗ trợ builder pattern
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberInfo { // Thông tin thành viên
        private Long userId; // ID thành viên
        private String username; // Tên thành viên
        private String email; // Email
        private String avatarUrl; // Ảnh đại diện
        private String role; // Vai trò
        private LocalDateTime joinedAt; // Thời gian tham gia
    }
}
