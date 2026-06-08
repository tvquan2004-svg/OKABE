package com.okabe.entity;

import com.okabe.entity.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity // Đánh dấu là entity JPA
@Table(name = "workspace_members") // Ánh xạ đến bảng workspace_members
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(WorkspaceMemberId.class) // Sử dụng khoá chính hợp thành
public class WorkspaceMember {

    @Id // Khoá chính hợp thành (phần 1)
    @Column(name = "workspace_id")
    private Long workspaceId; // ID không gian làm việc (khoá chính)

    @Id // Khoá chính hợp thành (phần 2)
    @Column(name = "user_id")
    private Long userId; // ID người dùng (khoá chính)

    @Enumerated(EnumType.STRING) // Lưu enum dạng chuỗi
    @Column(nullable = false, length = 20) // Vai trò trong workspace (bắt buộc)
    @Builder.Default
    private Role role = Role.MEMBER; // Vai trò của thành viên trong workspace

    @Column(name = "joined_at") // Thời gian tham gia
    @Builder.Default
    private LocalDateTime joinedAt = LocalDateTime.now(); // Thời điểm thành viên tham gia

    @ManyToOne(fetch = FetchType.LAZY) // Nhiều thành viên thuộc về một workspace
    @JoinColumn(name = "workspace_id", insertable = false, updatable = false) // Khoá ngoại (chỉ đọc)
    private Workspace workspace; // Không gian làm việc

    @ManyToOne(fetch = FetchType.LAZY) // Nhiều thành viên là một người dùng
    @JoinColumn(name = "user_id", insertable = false, updatable = false) // Khoá ngoại (chỉ đọc)
    private User user; // Người dùng thành viên
}
