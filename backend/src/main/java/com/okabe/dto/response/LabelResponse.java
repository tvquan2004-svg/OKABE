package com.okabe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder // Hỗ trợ builder pattern
@NoArgsConstructor
@AllArgsConstructor
public class LabelResponse {
    private Long id; // ID nhãn
    private Long boardId; // ID bảng chứa nhãn
    private String name; // Tên nhãn
    private String color; // Mã màu
}
