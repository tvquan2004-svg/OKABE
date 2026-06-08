package com.okabe.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity // Đánh dấu là entity JPA
@Table(name = "objectives") // Ánh xạ đến bảng objectives
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Objective {

    @Id // Khóa chính
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Tự động tăng
    private Long id; // ID duy nhất của mục tiêu

    @Column(name = "workspace_id", nullable = false) // ID không gian làm việc (bắt buộc)
    private Long workspaceId; // ID không gian làm việc chứa mục tiêu

    @Column(nullable = false, length = 500) // Tiêu đề mục tiêu (bắt buộc)
    private String title; // Tiêu đề của mục tiêu (OKR)

    @Column(columnDefinition = "TEXT") // Mô tả mục tiêu dạng văn bản dài
    private String description; // Mô tả chi tiết về mục tiêu

    @Column(nullable = false, length = 20) // Quý trong năm (bắt buộc)
    private String quarter; // Quý thực hiện (VD: "2025-Q1", "2025-Q2")

    @Column(nullable = false, precision = 5, scale = 2) // Tiến độ hoàn thành (bắt buộc)
    @Builder.Default
    private BigDecimal progress = new BigDecimal("0.00"); // Phần trăm tiến độ (0-100)

    @Column(name = "created_by", nullable = false) // ID người tạo (bắt buộc)
    private Long createdBy; // ID người dùng đã tạo mục tiêu

    @Column(name = "created_at", nullable = false, updatable = false) // Thời gian tạo (bắt buộc, không thể sửa)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now(); // Thời điểm tạo mục tiêu

    @Column(name = "updated_at", nullable = false) // Thời gian cập nhật (bắt buộc)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now(); // Thời điểm cập nhật mục tiêu gần nhất
}
