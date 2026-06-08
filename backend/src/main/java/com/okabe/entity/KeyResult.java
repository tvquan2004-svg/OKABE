package com.okabe.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity // Đánh dấu là entity JPA
@Table(name = "key_results") // Ánh xạ đến bảng key_results
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeyResult {

    @Id // Khóa chính
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Tự động tăng
    private Long id; // ID duy nhất của kết quả then chốt

    @Column(name = "objective_id", nullable = false) // ID mục tiêu (bắt buộc)
    private Long objectiveId; // ID mục tiêu OKR chứa kết quả then chốt này

    @Column(nullable = false, length = 500) // Tiêu đề (bắt buộc)
    private String title; // Tiêu đề của kết quả then chốt

    @Column(name = "target_value", precision = 14, scale = 4) // Giá trị mục tiêu
    private BigDecimal targetValue; // Giá trị cần đạt được

    @Column(name = "current_value", precision = 14, scale = 4) // Giá trị hiện tại
    @Builder.Default
    private BigDecimal currentValue = new BigDecimal("0.0000"); // Giá trị đã đạt được hiện tại

    @Column(length = 50) // Đơn vị đo
    @Builder.Default
    private String unit = "percent"; // Đơn vị (percent, number, currency...)

    @Column(name = "linked_cards", columnDefinition = "TEXT") // Danh sách thẻ liên kết (dạng text)
    private String linkedCards; // ID các thẻ được liên kết với kết quả then chốt

    @Column(name = "created_at", nullable = false, updatable = false) // Thời gian tạo (bắt buộc, không thể sửa)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now(); // Thời điểm tạo kết quả then chốt
}
