package com.okabe.dto.response;

import lombok.*;
import java.util.List;

@Getter
@Setter
@Builder // Hỗ trợ builder pattern
@NoArgsConstructor
@AllArgsConstructor
public class BoardTemplateResponse {
    private Long id; // ID mẫu bảng
    private String name; // Tên mẫu
    private String description; // Mô tả mẫu
    private Boolean isSystem; // Là mẫu hệ thống?
    private List<TemplateListResponse> lists; // Danh sách các list trong mẫu
}
