package com.okabe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder // Hỗ trợ builder pattern
@NoArgsConstructor
@AllArgsConstructor
public class CardResponse {
    private Long id; // ID thẻ
    private Long listId; // ID danh sách chứa thẻ
    private String title; // Tiêu đề thẻ
    private String description; // Mô tả thẻ
    private Integer position; // Vị trí trong danh sách
    private LocalDateTime dueDate; // Hạn chót
    private LocalDateTime startDate; // Ngày bắt đầu
    private String priority; // Mức độ ưu tiên
    private Boolean isArchived; // Đã lưu trữ?
    private Long createdById; // ID người tạo
    private String createdByName; // Tên người tạo
    private LocalDateTime createdAt; // Thời gian tạo

    private Integer totalFocusMinutes; // Tổng phút tập trung

    // Phase 2 fields (các trường giai đoạn 2)
    private List<LabelResponse> labels; // Danh sách nhãn
    private List<ChecklistResponse> checklists; // Danh sách checklist
    private List<UserResponse> members; // Danh sách thành viên được gán
    private List<AttachmentResponse> attachments; // Danh sách tệp đính kèm
}
