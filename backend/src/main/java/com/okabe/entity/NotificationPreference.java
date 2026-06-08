package com.okabe.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity // Đánh dấu là entity JPA
@Table(name = "notification_preferences") // Ánh xạ đến bảng notification_preferences
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference {

    @Id // Khóa chính
    @Column(name = "user_id") // ID người dùng (khóa chính)
    private Long userId; // ID người dùng (khóa chính, đồng thời là khoá ngoại)

    @OneToOne(fetch = FetchType.LAZY) // Một người dùng có một cấu hình thông báo
    @JoinColumn(name = "user_id", insertable = false, updatable = false) // Khoá ngoại đến bảng users (chỉ đọc)
    private User user; // Người dùng sở hữu cấu hình

    @Column(name = "email_assigned", nullable = false) // Nhận email khi được gán việc (bắt buộc)
    private boolean emailAssigned; // Có gửi email khi được gán thẻ không

    @Column(name = "email_mentioned", nullable = false) // Nhận email khi được đề cập (bắt buộc)
    private boolean emailMentioned; // Có gửi email khi được đề cập không

    @Column(name = "email_due_soon", nullable = false) // Nhận email khi đến hạn (bắt buộc)
    private boolean emailDueSoon; // Có gửi email nhắc nhở đến hạn không

    @Column(name = "email_invited", nullable = false) // Nhận email khi được mời (bắt buộc)
    private boolean emailInvited; // Có gửi email khi được mời vào workspace không

    @UpdateTimestamp // Tự động cập nhật thời gian
    @Column(name = "updated_at") // Thời gian cập nhật gần nhất
    private LocalDateTime updatedAt; // Thời điểm cập nhật cấu hình gần nhất

    // Manual Getters and Setters to avoid Lombok issues (do không dùng @Getter/@Setter)
    public Long getUserId() { return userId; } // Lấy ID người dùng
    public void setUserId(Long userId) { this.userId = userId; } // Gán ID người dùng

    public User getUser() { return user; } // Lấy thông tin người dùng
    public void setUser(User user) { this.user = user; } // Gán thông tin người dùng

    public boolean isEmailAssigned() { return emailAssigned; } // Kiểm tra nhận email khi được gán
    public void setEmailAssigned(boolean emailAssigned) { this.emailAssigned = emailAssigned; } // Bật/tắt email khi được gán

    public boolean isEmailMentioned() { return emailMentioned; } // Kiểm tra nhận email khi được đề cập
    public void setEmailMentioned(boolean emailMentioned) { this.emailMentioned = emailMentioned; } // Bật/tắt email khi được đề cập

    public boolean isEmailDueSoon() { return emailDueSoon; } // Kiểm tra nhận email đến hạn
    public void setEmailDueSoon(boolean emailDueSoon) { this.emailDueSoon = emailDueSoon; } // Bật/tắt email nhắc nhở đến hạn

    public boolean isEmailInvited() { return emailInvited; } // Kiểm tra nhận email khi được mời
    public void setEmailInvited(boolean emailInvited) { this.emailInvited = emailInvited; } // Bật/tắt email khi được mời

    public LocalDateTime getUpdatedAt() { return updatedAt; } // Lấy thời gian cập nhật
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; } // Gán thời gian cập nhật
}
