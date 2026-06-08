package com.okabe.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity // Đánh dấu là entity JPA
@Table(name = "template_lists") // Ánh xạ đến bảng template_lists
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateList extends BaseEntity {

    @Id // Khóa chính
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Tự động tăng
    private Long id; // ID duy nhất của danh sách mẫu

    @ManyToOne(fetch = FetchType.LAZY) // Nhiều danh sách mẫu thuộc về một mẫu bảng
    @JoinColumn(name = "template_id", nullable = false) // Khoá ngoại đến bảng board_templates
    private BoardTemplate template; // Mẫu bảng chứa danh sách này

    @Column(nullable = false) // Tên danh sách mẫu (bắt buộc)
    private String name; // Tên hiển thị của danh sách trong mẫu

    @Column(nullable = false) // Vị trí sắp xếp (bắt buộc)
    private Integer position; // Vị trí của danh sách trong mẫu bảng

    @OneToMany(mappedBy = "templateList", cascade = CascadeType.ALL, orphanRemoval = true) // Một danh sách mẫu có nhiều thẻ mẫu
    private List<TemplateCard> cards; // Danh sách các thẻ mẫu trong danh sách
}
