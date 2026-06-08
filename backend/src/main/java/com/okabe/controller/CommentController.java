package com.okabe.controller;

import com.okabe.dto.request.CommentRequest;
import com.okabe.dto.response.ApiResponse;
import com.okabe.dto.response.CommentResponse;
import com.okabe.security.UserPrincipal;
import com.okabe.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Comment", description = "Card comment management APIs")
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/api/v1/cards/{cardId}/comments")
    @Operation(summary = "Create a comment on a card")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @PathVariable Long cardId,
            @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(commentService.createComment(cardId, request, currentUser))); // Tạo bình luận trên thẻ
    }

    @PutMapping("/api/v1/comments/{id}")
    @Operation(summary = "Update a comment")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @PathVariable Long id,
            @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(commentService.updateComment(id, request, currentUser))); // Cập nhật bình luận
    }

    @DeleteMapping("/api/v1/comments/{id}")
    @Operation(summary = "Delete a comment")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        commentService.deleteComment(id, currentUser); // Xoá bình luận
        return ResponseEntity.ok(ApiResponse.success(null, "Comment deleted"));
    }

    @GetMapping("/api/v1/cards/{cardId}/comments")
    @Operation(summary = "Get comments on a card")
    public ResponseEntity<ApiResponse<Page<CommentResponse>>> getCardComments(
            @PathVariable Long cardId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(commentService.getCardComments(cardId, pageable))); // Lấy danh sách bình luận của thẻ
    }
}
