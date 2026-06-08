package com.okabe.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity // Đánh dấu là entity JPA
@Table(name = "lists") // Ánh xạ đến bảng lists
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskList extends BaseEntity {

    @Id // Khóa chính
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Tự động tăng
    private Long id; // ID duy nhất của danh sách

    @ManyToOne(fetch = FetchType.LAZY) // Nhiều danh sách thuộc về một bảng
    @JoinColumn(name = "board_id", nullable = false) // Khoá ngoại đến bảng boards
    private Board board; // Bảng chứa danh sách

    @Column(nullable = false, length = 255) // Tên danh sách (bắt buộc)
    private String name; // Tên hiển thị của danh sách (VD: "Cần làm", "Đang làm")

    @Column(nullable = false) // Vị trí sắp xếp (bắt buộc)
    private Integer position; // Vị trí của danh sách trong bảng

    @Column(name = "is_archived", nullable = false) // Trạng thái lưu trữ (bắt buộc)
    @Builder.Default
    private Boolean isArchived = false; // Danh sách có bị lưu trữ không
}
