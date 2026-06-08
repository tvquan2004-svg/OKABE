package com.okabe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder // Hỗ trợ builder pattern
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistResponse {
    private Long id; // ID checklist
    private Long cardId; // ID thẻ chứa checklist
    private String name; // Tên checklist
    private Integer position; // Vị trí trong thẻ
    private List<ChecklistItemResponse> items; // Danh sách các mục
}
