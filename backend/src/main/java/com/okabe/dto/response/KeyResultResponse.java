package com.okabe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeyResultResponse {
    private Long id;
    private String title;
    private BigDecimal targetValue;
    private BigDecimal currentValue;
    private String unit;
    private List<Long> linkedCards;
    private LocalDateTime createdAt;
}
