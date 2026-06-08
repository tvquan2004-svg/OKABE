package com.okabe.dto.response;

import lombok.*;
import java.util.List;

@Getter
@Setter
@Builder // Hỗ trợ builder pattern
@NoArgsConstructor
@AllArgsConstructor
public class TemplateListResponse {
    private Long id; // ID danh sách mẫu
    private String name; // Tên danh sách mẫu
    private Integer position; // Vị trí
    private List<TemplateCardResponse> cards; // Danh sách thẻ mẫu
}
