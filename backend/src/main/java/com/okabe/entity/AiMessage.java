package com.okabe.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity // Đánh dấu là entity JPA
@Table(name = "ai_messages") // Ánh xạ đến bảng ai_messages
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiMessage {

    @Id // Khóa chính
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Tự động tăng
    private Long id; // ID duy nhất của tin nhắn

    @ManyToOne(fetch = FetchType.LAZY) // Nhiều tin nhắn thuộc về một cuộc trò chuyện
    @JoinColumn(name = "conversation_id", nullable = false) // Khoá ngoại đến bảng ai_conversations
    private AiConversation conversation; // Cuộc trò chuyện chứa tin nhắn

    @Enumerated(EnumType.STRING) // Lưu enum dạng chuỗi
    @Column(nullable = false, length = 20) // Vai trò người gửi (bắt buộc)
    private MessageRole role; // Vai trò người gửi: USER, ASSISTANT, SYSTEM

    @Column(nullable = false, columnDefinition = "TEXT") // Nội dung tin nhắn (bắt buộc, dạng văn bản dài)
    private String content; // Nội dung tin nhắn

    @Column(name = "tokens_used") // Số token đã sử dụng
    @Builder.Default
    private Integer tokensUsed = 0; // Số lượng token tiêu thụ cho tin nhắn này

    @Column(name = "created_at", updatable = false) // Thời gian tạo (không thể sửa)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now(); // Thời gian gửi tin nhắn

    public enum MessageRole {
        USER,      // Người dùng
        ASSISTANT, // Trợ lý AI
        SYSTEM     // Hệ thống
    }
}
