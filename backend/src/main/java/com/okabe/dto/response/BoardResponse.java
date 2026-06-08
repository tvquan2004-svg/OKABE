package com.okabe.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder // Hỗ trợ builder pattern
@NoArgsConstructor
@AllArgsConstructor
public class BoardResponse {
    private Long id; // ID bảng
    private Long workspaceId; // ID không gian làm việc
    private String name; // Tên bảng
    private String description; // Mô tả bảng
    private Integer position; // Vị trí sắp xếp
    private String background; // Ảnh nền
    private Boolean isStarred; // Đã gắn sao?
    private Boolean isArchived; // Đã lưu trữ?
    private Boolean isPublic; // Công khai?
    private String publicToken; // Token chia sẻ công khai
    private int listCount; // Số lượng danh sách
    private int totalCards; // Tổng số thẻ
    private LocalDateTime createdAt; // Thời gian tạo
    private List<ListResponse> lists; // Danh sách các list
}
