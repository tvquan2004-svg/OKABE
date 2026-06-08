package com.okabe.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity // Đánh dấu là entity JPA
@Table(name = "checklists") // Ánh xạ đến bảng checklists
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Checklist extends BaseEntity {

    @Id // Khóa chính
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Tự động tăng
    private Long id; // ID duy nhất của checklist

    @ManyToOne(fetch = FetchType.LAZY) // Nhiều checklist thuộc về một thẻ
    @JoinColumn(name = "card_id", nullable = false) // Khoá ngoại đến bảng cards
    private Card card; // Thẻ chứa checklist

    @Column(nullable = false, length = 100) // Tên checklist (bắt buộc)
    private String name; // Tên của checklist (VD: "Các bước cần làm")

    @Column(nullable = false) // Vị trí sắp xếp (bắt buộc)
    @Builder.Default
    private Integer position = 0; // Vị trí của checklist trong thẻ

    @OneToMany(mappedBy = "checklist", cascade = CascadeType.ALL, orphanRemoval = true) // Một checklist có nhiều mục con
    @OrderBy("position ASC") // Sắp xếp các mục theo vị trí tăng dần
    @Builder.Default
    private List<ChecklistItem> items = new ArrayList<>(); // Danh sách các mục trong checklist
}
