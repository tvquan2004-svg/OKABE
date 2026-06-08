package com.okabe.dto.response;

import lombok.*;

@Getter
@Setter
@Builder // Hỗ trợ builder pattern
@NoArgsConstructor
@AllArgsConstructor
public class TemplateCardResponse {
    private Long id; // ID thẻ mẫu
    private String title; // Tiêu đề thẻ mẫu
    private String description; // Mô tả thẻ mẫu
    private Integer position; // Vị trí
}
