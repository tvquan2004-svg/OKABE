package com.okabe.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity // Đánh dấu là entity JPA
@Table(name = "checklist_items") // Ánh xạ đến bảng checklist_items
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChecklistItem extends BaseEntity {

    @Id // Khóa chính
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Tự động tăng
    private Long id; // ID duy nhất của mục checklist

    @ManyToOne(fetch = FetchType.LAZY) // Nhiều mục thuộc về một checklist
    @JoinColumn(name = "checklist_id", nullable = false) // Khoá ngoại đến bảng checklists
    private Checklist checklist; // Checklist chứa mục này

    @Column(nullable = false, length = 500) // Nội dung mục (bắt buộc)
    private String content; // Nội dung của mục checklist

    @Column(name = "is_completed", nullable = false) // Trạng thái hoàn thành (bắt buộc)
    @Builder.Default
    private Boolean isCompleted = false; // Mục đã được hoàn thành chưa

    @Column(nullable = false) // Vị trí sắp xếp (bắt buộc)
    @Builder.Default
    private Integer position = 0; // Vị trí của mục trong checklist
}
