package com.okabe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder // Hỗ trợ builder pattern
@NoArgsConstructor
@AllArgsConstructor
public class CardInfoResponse {
    private Long id; // ID thẻ
    private Long listId; // ID danh sách chứa thẻ
    private String title; // Tiêu đề thẻ
    private String priority; // Mức độ ưu tiên
    private Boolean isArchived; // Đã lưu trữ?
    private LocalDateTime dueDate; // Hạn chót
    private String listName; // Tên danh sách
    private String boardName; // Tên bảng
}
