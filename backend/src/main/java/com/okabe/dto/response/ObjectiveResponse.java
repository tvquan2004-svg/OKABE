package com.okabe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder // Hỗ trợ builder pattern
@NoArgsConstructor
@AllArgsConstructor
public class ObjectiveResponse {
    private Long id; // ID mục tiêu
    private String title; // Tiêu đề mục tiêu
    private String description; // Mô tả
    private String quarter; // Quý thực hiện
    private BigDecimal progress; // Tiến độ (%)
    private Long createdBy; // ID người tạo
    private LocalDateTime createdAt; // Thời gian tạo
    private LocalDateTime updatedAt; // Thời gian cập nhật
    private List<KeyResultResponse> keyResults; // Danh sách kết quả then chốt
}
