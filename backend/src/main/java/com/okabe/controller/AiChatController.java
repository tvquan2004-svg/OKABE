package com.okabe.controller;

import com.okabe.dto.request.ChatRequest;
import com.okabe.dto.response.ApiResponse;
import com.okabe.dto.response.ChatResponse;
import com.okabe.dto.response.ConversationResponse;
import com.okabe.dto.response.MessageResponse;
import com.okabe.security.UserPrincipal;
import com.okabe.service.AiChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI Chat", description = "AI Assistant chatbox APIs")
public class AiChatController {

    private final AiChatService aiChatService;

    @PostMapping("/conversations")
    @Operation(summary = "Tạo cuộc hội thoại mới")
    public ResponseEntity<ApiResponse<ConversationResponse>> createConversation(
            @RequestParam(required = false) Long boardId,
            @RequestParam(required = false) Long workspaceId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        aiChatService.createConversation(boardId, workspaceId, currentUser),
                        "Đã tạo cuộc hội thoại mới"));
    }

    @GetMapping("/conversations")
    @Operation(summary = "Lấy danh sách cuộc hội thoại của user")
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> getConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                aiChatService.getConversations(page, size, currentUser)));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "Lấy tất cả tin nhắn trong một cuộc hội thoại")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getMessages(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                aiChatService.getMessages(conversationId, currentUser)));
    }

    @PostMapping("/chat")
    @Operation(summary = "Gửi tin nhắn đến AI và nhận phản hồi")
    public ResponseEntity<ApiResponse<ChatResponse>> sendMessage(
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                aiChatService.sendMessage(request, currentUser)));
    }

    @DeleteMapping("/conversations/{conversationId}")
    @Operation(summary = "Xoá một cuộc hội thoại")
    public ResponseEntity<ApiResponse<Void>> deleteConversation(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        aiChatService.deleteConversation(conversationId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xoá cuộc hội thoại"));
    }
}
