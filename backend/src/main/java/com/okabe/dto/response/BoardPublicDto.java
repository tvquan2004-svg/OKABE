package com.okabe.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder // Hỗ trợ builder pattern
@NoArgsConstructor
@AllArgsConstructor
public class BoardPublicDto {
    private Long id; // ID bảng
    private String name; // Tên bảng
    private String description; // Mô tả bảng
    private String background; // Ảnh nền bảng
    private LocalDateTime createdAt; // Thời gian tạo
    private List<ListResponse> lists; // Danh sách các list trong bảng

    // Member info without email (thông tin thành viên không bao gồm email)
    private List<PublicUserResponse> members; // Danh sách thành viên (công khai)

    @Getter
    @Builder // Hỗ trợ builder pattern
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PublicUserResponse { // Thông tin người dùng công khai (không email)
        private Long id; // ID người dùng
        private String username; // Tên người dùng
        private String avatarUrl; // URL ảnh đại diện
    }
}
