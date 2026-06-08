package com.okabe.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity // Đánh dấu là entity JPA
@Table(name = "board_templates") // Ánh xạ đến bảng board_templates
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardTemplate extends BaseEntity {

    @Id // Khóa chính
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Tự động tăng
    private Long id; // ID duy nhất của mẫu bảng

    @Column(nullable = false) // Tên mẫu (bắt buộc)
    private String name; // Tên của mẫu bảng

    @Column(columnDefinition = "TEXT") // Mô tả mẫu dạng văn bản dài
    private String description; // Mô tả chi tiết về mẫu

    @Column(name = "is_system") // Là mẫu hệ thống hay do người dùng tạo
    @Builder.Default
    private Boolean isSystem = false; // Mẫu có phải do hệ thống cung cấp không

    @ManyToOne(fetch = FetchType.LAZY) // Nhiều mẫu được tạo bởi một người dùng
    @JoinColumn(name = "created_by") // Khoá ngoại đến bảng users
    private User createdBy; // Người dùng đã tạo mẫu này

    @ManyToOne(fetch = FetchType.LAZY) // Nhiều mẫu thuộc về một không gian làm việc
    @JoinColumn(name = "workspace_id") // Khoá ngoại đến bảng workspaces
    private Workspace workspace; // Không gian làm việc chứa mẫu

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true) // Một mẫu có nhiều danh sách mẫu
    private List<TemplateList> lists; // Danh sách các danh sách trong mẫu
}
