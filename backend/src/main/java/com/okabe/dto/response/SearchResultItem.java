package com.okabe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder // Hỗ trợ builder pattern
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultItem {
    private String id; // ID kết quả tìm kiếm
    private String type; // Loại kết quả (card, board, list...)
    private String title; // Tiêu đề kết quả
    private String subtitle; // Phụ đề
    private String breadcrumb; // Đường dẫn phân cấp
    private String url; // Đường dẫn đến kết quả
    private String icon; // Biểu tượng
}
