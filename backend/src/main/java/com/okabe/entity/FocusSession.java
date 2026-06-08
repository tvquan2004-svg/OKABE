package com.okabe.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity // Đánh dấu là entity JPA
@Table(name = "focus_sessions") // Ánh xạ đến bảng focus_sessions
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FocusSession extends BaseEntity {

    @Id // Khóa chính
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Tự động tăng
    private Long id; // ID duy nhất của phiên tập trung

    @Column(name = "card_id", nullable = false) // ID thẻ (bắt buộc)
    private Long cardId; // ID thẻ đang được tập trung làm việc

    @Column(name = "user_id", nullable = false) // ID người dùng (bắt buộc)
    private Long userId; // ID người dùng thực hiện phiên tập trung

    @Column(name = "started_at", nullable = false) // Thời gian bắt đầu (bắt buộc)
    private LocalDateTime startedAt; // Thời điểm bắt đầu phiên tập trung

    @Column(name = "ended_at") // Thời gian kết thúc
    private LocalDateTime endedAt; // Thời điểm kết thúc phiên tập trung

    @Column(name = "duration_minutes") // Thời lượng (phút)
    @Builder.Default
    private Integer durationMinutes = 25; // Thời lượng mặc định của phiên (phút)

    @Column(nullable = false) // Trạng thái hoàn thành (bắt buộc)
    @Builder.Default
    private Boolean completed = false; // Phiên tập trung đã hoàn thành chưa
}
