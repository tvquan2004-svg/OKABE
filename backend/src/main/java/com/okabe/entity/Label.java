package com.okabe.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity // Đánh dấu là entity JPA
@Table(name = "labels") // Ánh xạ đến bảng labels
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Label extends BaseEntity {

    @Id // Khóa chính
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Tự động tăng
    private Long id; // ID duy nhất của nhãn

    @ManyToOne(fetch = FetchType.LAZY) // Nhiều nhãn thuộc về một bảng
    @JoinColumn(name = "board_id", nullable = false) // Khoá ngoại đến bảng boards
    private Board board; // Bảng chứa nhãn

    @Column(nullable = false, length = 100) // Tên nhãn (bắt buộc)
    private String name; // Tên hiển thị của nhãn (VD: "Quan trọng", "Bug")

    @Column(nullable = false, length = 20) // Màu sắc (bắt buộc)
    private String color; // Mã màu của nhãn (VD: "#FF0000")

    @ManyToMany(mappedBy = "labels") // Nhiều-nhiều với thẻ (phía ngược lại)
    @Builder.Default
    private Set<Card> cards = new HashSet<>(); // Danh sách thẻ được gán nhãn này
}
