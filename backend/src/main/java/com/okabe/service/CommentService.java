package com.okabe.service;

import com.okabe.dto.request.CommentRequest;
import com.okabe.dto.response.CommentResponse;
import com.okabe.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommentService {
    // Tạo bình luận mới trong thẻ
    CommentResponse createComment(Long cardId, CommentRequest request, UserPrincipal currentUser);
    // Cập nhật bình luận
    CommentResponse updateComment(Long commentId, CommentRequest request, UserPrincipal currentUser);
    // Xoá bình luận
    void deleteComment(Long commentId, UserPrincipal currentUser);
    // Lấy danh sách bình luận của thẻ (có phân trang)
    Page<CommentResponse> getCardComments(Long cardId, Pageable pageable);
}
