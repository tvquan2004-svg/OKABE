package com.okabe.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity // Đánh dấu là entity JPA
@Table(name = "boards") // Ánh xạ đến bảng boards
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Board extends BaseEntity {

    @Id // Khóa chính
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Tự động tăng
    private Long id; // ID duy nhất của bảng

    @ManyToOne(fetch = FetchType.LAZY) // Nhiều bảng thuộc về một không gian làm việc
    @JoinColumn(name = "workspace_id", nullable = false) // Khoá ngoại đến bảng workspaces
    private Workspace workspace; // Không gian làm việc chứa bảng

    @Column(nullable = false, length = 255) // Tên bảng (bắt buộc)
    private String name; // Tên của bảng

    @Column(columnDefinition = "TEXT") // Mô tả bảng dạng văn bản dài
    private String description; // Mô tả chi tiết về bảng

    @Column(nullable = false) // Vị trí sắp xếp (bắt buộc)
    @Builder.Default
    private Integer position = 0; // Vị trí của bảng trong danh sách

    @Column(length = 255) // Đường dẫn ảnh nền
    private String background; // Ảnh nền hoặc màu nền của bảng

    @Column(name = "is_starred", nullable = false) // Trạng thái đánh dấu sao (bắt buộc)
    @Builder.Default
    private Boolean isStarred = false; // Bảng có được gắn dấu sao không

    @Column(name = "is_archived", nullable = false) // Trạng thái lưu trữ (bắt buộc)
    @Builder.Default
    private Boolean isArchived = false; // Bảng có bị lưu trữ không

    @Column(name = "is_public", nullable = false) // Trạng thái công khai (bắt buộc)
    @Builder.Default
    private Boolean isPublic = false; // Bảng có được công khai không

    @Column(name = "public_token", unique = true, length = 64) // Token chia sẻ công khai (duy nhất)
    private String publicToken; // Token để chia sẻ bảng công khai qua đường dẫn
}
