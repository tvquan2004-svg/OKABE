package com.okabe.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity // Đánh dấu là entity JPA
@Table(name = "ai_conversations") // Ánh xạ đến bảng ai_conversations
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiConversation {

    @Id // Khóa chính
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Tự động tăng
    private Long id; // ID duy nhất của cuộc trò chuyện

    @ManyToOne(fetch = FetchType.LAZY) // Nhiều cuộc trò chuyện thuộc về một người dùng
    @JoinColumn(name = "user_id", nullable = false) // Khoá ngoại đến bảng users
    private User user; // Người dùng tham gia cuộc trò chuyện

    @Column(name = "board_id") // ID bảng liên quan (có thể null)
    private Long boardId; // ID bảng được liên kết với cuộc trò chuyện

    @Column(name = "workspace_id") // ID không gian làm việc (có thể null)
    private Long workspaceId; // ID không gian làm việc được liên kết

    @Column(nullable = false, length = 255) // Tiêu đề cuộc trò chuyện (bắt buộc)
    @Builder.Default
    private String title = "Cuộc trò chuyện mới"; // Tiêu đề của cuộc trò chuyện

    @Column(name = "created_at", updatable = false) // Thời gian tạo (không thể sửa)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now(); // Thời gian tạo cuộc trò chuyện

    @Column(name = "updated_at") // Thời gian cập nhật
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now(); // Thời gian cập nhật cuộc trò chuyện

    @PreUpdate // Tự động cập nhật thời gian trước khi lưu
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
