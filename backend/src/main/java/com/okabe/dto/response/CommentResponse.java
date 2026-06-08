package com.okabe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder // Hỗ trợ builder pattern
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private Long id; // ID bình luận
    private Long cardId; // ID thẻ được bình luận
    private UserResponse author; // Người viết bình luận
    private String content; // Nội dung bình luận
    private Boolean isEdited; // Đã chỉnh sửa?
    private Set<UserResponse> mentions; // Danh sách người được đề cập
    private LocalDateTime createdAt; // Thời gian tạo
    private LocalDateTime updatedAt; // Thời gian cập nhật
}
