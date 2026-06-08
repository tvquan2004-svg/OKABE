package com.okabe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder // Hỗ trợ builder pattern
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistItemResponse {
    private Long id; // ID mục checklist
    private Long checklistId; // ID checklist chứa mục
    private String content; // Nội dung mục
    private Boolean isCompleted; // Đã hoàn thành?
    private Integer position; // Vị trí trong checklist
}
