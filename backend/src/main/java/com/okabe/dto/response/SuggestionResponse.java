package com.okabe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder // Hỗ trợ builder pattern
@NoArgsConstructor
@AllArgsConstructor
public class SuggestionResponse {
    private Long id; // ID gợi ý
    private String type; // Loại gợi ý
    private String message; // Nội dung gợi ý
    private Long cardId; // ID thẻ liên quan
    private String actionUrl; // Đường dẫn hành động
}
