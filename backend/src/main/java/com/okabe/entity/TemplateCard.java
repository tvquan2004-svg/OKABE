package com.okabe.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity // Đánh dấu là entity JPA
@Table(name = "template_cards") // Ánh xạ đến bảng template_cards
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateCard extends BaseEntity {

    @Id // Khóa chính
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Tự động tăng
    private Long id; // ID duy nhất của thẻ mẫu

    @ManyToOne(fetch = FetchType.LAZY) // Nhiều thẻ mẫu thuộc về một danh sách mẫu
    @JoinColumn(name = "template_list_id", nullable = false) // Khoá ngoại đến bảng template_lists
    private TemplateList templateList; // Danh sách mẫu chứa thẻ

    @Column(nullable = false, length = 500) // Tiêu đề thẻ mẫu (bắt buộc)
    private String title; // Tiêu đề của thẻ trong mẫu

    @Column(columnDefinition = "TEXT") // Mô tả thẻ mẫu dạng văn bản dài
    private String description; // Mô tả chi tiết của thẻ trong mẫu

    @Column(nullable = false) // Vị trí sắp xếp (bắt buộc)
    private Integer position; // Vị trí của thẻ trong danh sách mẫu
}
