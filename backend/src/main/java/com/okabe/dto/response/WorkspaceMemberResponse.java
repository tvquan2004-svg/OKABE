package com.okabe.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder // Hỗ trợ builder pattern
public class WorkspaceMemberResponse {
    private Long userId; // ID thành viên
    private String username; // Tên thành viên
    private String email; // Email thành viên
    private String avatarUrl; // Ảnh đại diện
    private String role; // Vai trò (OWNER, ADMIN, MEMBER)
    private LocalDateTime joinedAt; // Thời gian tham gia
}
