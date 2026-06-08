package com.okabe.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity // Đánh dấu là entity JPA
@Table(name = "dismissed_suggestions") // Ánh xạ đến bảng dismissed_suggestions
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DismissedSuggestion {

    @Id // Khóa chính
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Tự động tăng
    private Long id; // ID duy nhất của gợi ý đã tắt

    @Column(name = "user_id", nullable = false) // ID người dùng (bắt buộc)
    private Long userId; // ID người dùng đã tắt gợi ý

    @Column(name = "workspace_id", nullable = false) // ID không gian làm việc (bắt buộc)
    private Long workspaceId; // ID không gian làm việc của gợi ý

    @Column(nullable = false, length = 50) // Loại gợi ý (bắt buộc)
    private String type; // Loại gợi ý (VD: assign_suggestion, priority_suggestion)

    @Column(name = "card_id") // ID thẻ liên quan (có thể null)
    private Long cardId; // ID thẻ được gợi ý

    @Column(name = "dismissed_at", nullable = false, updatable = false) // Thời gian tắt (bắt buộc, không thể sửa)
    @Builder.Default
    private LocalDateTime dismissedAt = LocalDateTime.now(); // Thời điểm người dùng tắt gợi ý
}
