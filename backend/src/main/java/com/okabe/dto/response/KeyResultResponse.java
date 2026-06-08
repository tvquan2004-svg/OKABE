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
public class KeyResultResponse {
    private Long id; // ID kết quả then chốt
    private String title; // Tiêu đề
    private BigDecimal targetValue; // Giá trị mục tiêu
    private BigDecimal currentValue; // Giá trị hiện tại
    private String unit; // Đơn vị
    private List<Long> linkedCards; // Danh sách ID thẻ liên kết
    private LocalDateTime createdAt; // Thời gian tạo
}
