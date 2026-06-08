package com.okabe.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity // Đánh dấu là entity JPA
@Table(name = "notifications") // Ánh xạ đến bảng notifications
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id // Khóa chính
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Tự động tăng
    private Long id; // ID duy nhất của thông báo

    @ManyToOne(fetch = FetchType.LAZY) // Nhiều thông báo gửi đến một người nhận
    @JoinColumn(name = "recipient_id", nullable = false) // Khoá ngoại đến bảng users
    private User recipient; // Người nhận thông báo

    @ManyToOne(fetch = FetchType.LAZY) // Nhiều thông báo được kích hoạt bởi một người dùng
    @JoinColumn(name = "actor_id") // Khoá ngoại đến bảng users
    private User actor; // Người thực hiện hành động gây ra thông báo

    @Column(nullable = false, length = 100) // Loại thông báo (bắt buộc)
    private String type; // Loại thông báo (VD: mention, assign, due_date)

    @Column(name = "entity_type", nullable = false, length = 50) // Loại đối tượng (bắt buộc)
    private String entityType; // Loại đối tượng liên quan (card, board, comment...)

    @Column(name = "entity_id", nullable = false) // ID đối tượng (bắt buộc)
    private Long entityId; // ID của đối tượng liên quan

    @Column(name = "extra_id") // ID bổ sung
    private Long extraId; // ID đối tượng phụ (VD: comment_id trong thông báo comment)

    @Column(nullable = false, length = 500) // Nội dung thông báo (bắt buộc)
    private String message; // Nội dung chi tiết của thông báo

    @Column(name = "is_read", nullable = false) // Trạng thái đã đọc (bắt buộc)
    @Builder.Default
    private Boolean isRead = false; // Thông báo đã được đọc chưa

    @Column(name = "created_at", nullable = false, updatable = false) // Thời gian tạo (bắt buộc, không thể sửa)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now(); // Thời điểm tạo thông báo
}
