package com.okabe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder // Hỗ trợ builder pattern
@NoArgsConstructor
@AllArgsConstructor
public class CardSelectionResponse {
    private Long id; // ID thẻ
    private String title; // Tiêu đề thẻ
    private Long boardId; // ID bảng chứa thẻ
    private String boardName; // Tên bảng
    private String listName; // Tên danh sách
}
