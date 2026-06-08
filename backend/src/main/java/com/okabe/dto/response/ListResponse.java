package com.okabe.dto.response;

import lombok.*;
import java.util.List;

@Getter
@Builder // Hỗ trợ builder pattern
@NoArgsConstructor
@AllArgsConstructor
public class ListResponse {
    private Long id; // ID danh sách
    private Long boardId; // ID bảng chứa danh sách
    private String name; // Tên danh sách
    private Integer position; // Vị trí
    private List<CardResponse> cards; // Danh sách thẻ trong danh sách
}
