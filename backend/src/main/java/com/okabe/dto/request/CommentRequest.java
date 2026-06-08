package com.okabe.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder // Hỗ trợ builder pattern
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequest {
    
    @NotBlank(message = "Comment content is required") // Nội dung bình luận không được để trống
    @Size(max = 2000, message = "Comment must not exceed 2000 characters") // Giới hạn 2000 ký tự
    private String content; // Nội dung bình luận
}
