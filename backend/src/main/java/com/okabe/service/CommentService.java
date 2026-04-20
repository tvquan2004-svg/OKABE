package com.okabe.service;

import com.okabe.dto.request.CommentRequest;
import com.okabe.dto.response.CommentResponse;
import com.okabe.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommentService {
    CommentResponse createComment(Long cardId, CommentRequest request, UserPrincipal currentUser);
    CommentResponse updateComment(Long commentId, CommentRequest request, UserPrincipal currentUser);
    void deleteComment(Long commentId, UserPrincipal currentUser);
    Page<CommentResponse> getCardComments(Long cardId, Pageable pageable);
}
