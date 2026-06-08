package com.okabe.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder // Hỗ trợ builder pattern
@NoArgsConstructor
@AllArgsConstructor
public class SaveAsTemplateRequest {
    @NotBlank(message = "Template name is required") // Tên mẫu không được để trống
    private String name; // Tên của mẫu bảng
    private String description; // Mô tả mẫu (không bắt buộc)
}
