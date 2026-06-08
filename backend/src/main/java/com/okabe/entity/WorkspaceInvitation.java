package com.okabe.entity;

import com.okabe.entity.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity // Đánh dấu là entity JPA
@Table(name = "workspace_invitations") // Ánh xạ đến bảng workspace_invitations
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceInvitation {

    @Id // Khóa chính
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Tự động tăng
    private Long id; // ID duy nhất của lời mời

    @Column(name = "workspace_id", nullable = false) // ID không gian làm việc (bắt buộc)
    private Long workspaceId; // ID không gian làm việc được mời vào

    @ManyToOne(fetch = FetchType.LAZY) // Nhiều lời mời thuộc về một không gian làm việc
    @JoinColumn(name = "workspace_id", insertable = false, updatable = false) // Khoá ngoại (chỉ đọc)
    private Workspace workspace; // Không gian làm việc được mời

    @Column(nullable = false) // Email người được mời (bắt buộc)
    private String email; // Địa chỉ email của người được mời

    @Column(nullable = false) // Vai trò được mời (bắt buộc)
    @Enumerated(EnumType.STRING) // Lưu enum dạng chuỗi
    private Role role; // Vai trò sẽ được gán khi chấp nhận lời mời

    @Column(unique = true, nullable = false) // Token duy nhất (bắt buộc)
    private String token; // Token duy nhất để xác thực lời mời

    @Column(name = "inviter_id", nullable = false) // ID người mời (bắt buộc)
    private Long inviterId; // ID người dùng đã gửi lời mời

    @ManyToOne(fetch = FetchType.LAZY) // Nhiều lời mời được gửi bởi một người dùng
    @JoinColumn(name = "inviter_id", insertable = false, updatable = false) // Khoá ngoại (chỉ đọc)
    private User inviter; // Người dùng đã gửi lời mời

    @Builder.Default
    @Column(nullable = false) // Trạng thái lời mời (bắt buộc)
    private String status = "PENDING"; // Trạng thái: PENDING, ACCEPTED, REJECTED, EXPIRED

    @CreationTimestamp // Tự động gán thời gian tạo
    @Column(name = "created_at") // Thời gian tạo lời mời
    private LocalDateTime createdAt; // Thời điểm tạo lời mời

    @Column(name = "expires_at") // Thời gian hết hạn
    private LocalDateTime expiresAt; // Thời điểm lời mời hết hạn
}
