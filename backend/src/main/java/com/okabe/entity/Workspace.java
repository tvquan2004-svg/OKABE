package com.okabe.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity // Đánh dấu là entity JPA
@Table(name = "workspaces") // Ánh xạ đến bảng workspaces
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Workspace extends BaseEntity {

    @Id // Khóa chính
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Tự động tăng
    private Long id; // ID duy nhất của không gian làm việc

    @Column(nullable = false, length = 255) // Tên không gian làm việc (bắt buộc)
    private String name; // Tên hiển thị của không gian làm việc

    @Column(nullable = false, unique = true, length = 255) // Đường dẫn slug (bắt buộc, duy nhất)
    private String slug; // Slug dùng cho URL (VD: "my-workspace")

    @Column(columnDefinition = "TEXT") // Mô tả không gian làm việc dạng văn bản dài
    private String description; // Mô tả chi tiết về không gian làm việc

    @ManyToOne(fetch = FetchType.LAZY) // Nhiều không gian làm việc thuộc về một chủ sở hữu
    @JoinColumn(name = "owner_id", nullable = false) // Khoá ngoại đến bảng users
    private User owner; // Chủ sở hữu của không gian làm việc
}
