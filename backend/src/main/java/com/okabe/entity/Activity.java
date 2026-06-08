package com.okabe.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity // Đánh dấu là entity JPA
@Table(name = "activities") // Ánh xạ đến bảng activities
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Activity extends BaseEntity {

    @Id // Khóa chính
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Tự động tăng
    private Long id; // ID duy nhất của hoạt động

    @ManyToOne(fetch = FetchType.LAZY) // Nhiều hoạt động thuộc về một thẻ
    @JoinColumn(name = "card_id", nullable = false) // Khoá ngoại đến bảng cards
    private Card card; // Thẻ liên quan đến hoạt động

    @ManyToOne(fetch = FetchType.LAZY) // Nhiều hoạt động thuộc về một người dùng
    @JoinColumn(name = "user_id", nullable = false) // Khoá ngoại đến bảng users
    private User user; // Người dùng thực hiện hoạt động

    @Column(name = "action_type", nullable = false, length = 50) // Loại hành động (VD: create_card, move_card)
    private String actionType; // Loại hành động đã thực hiện

    @Column(columnDefinition = "TEXT") // Mô tả chi tiết dạng văn bản dài
    private String description; // Mô tả chi tiết hoạt động
}
