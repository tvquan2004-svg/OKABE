package com.okabe.controller;

import com.okabe.dto.request.BreakdownRequest;
import com.okabe.dto.request.ChatRequest;
import com.okabe.dto.request.SuggestPriorityRequest;
import com.okabe.dto.response.ApiResponse;
import com.okabe.dto.response.ChatResponse;
import com.okabe.dto.response.ConversationResponse;
import com.okabe.dto.response.MessageResponse;
import com.okabe.dto.response.PrioritySuggestion;
import com.okabe.dto.response.SubtaskSuggestion;
import com.okabe.security.UserPrincipal;
import com.okabe.service.AiChatService;
import com.okabe.service.AiPriorityService;
import com.okabe.service.AiTaskBreakdownService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI Chat", description = "AI Assistant chatbox APIs")
public class AiChatController {

    private final AiChatService aiChatService;
    private final AiTaskBreakdownService aiTaskBreakdownService;
    private final AiPriorityService aiPriorityService;

    @PostMapping("/breakdown")
    @Operation(summary = "Phân rã task thành các subtask bằng AI")
    public ResponseEntity<ApiResponse<List<SubtaskSuggestion>>> breakdownTask(
            @Valid @RequestBody BreakdownRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<SubtaskSuggestion> suggestions = aiTaskBreakdownService.breakdownTask(request.cardId());
        return ResponseEntity.ok(ApiResponse.success(suggestions));
    }

    @PostMapping("/suggest-priority")
    @Operation(summary = "Gợi ý độ ưu tiên cho card dựa trên context")
    public ResponseEntity<ApiResponse<PrioritySuggestion>> suggestPriority(
            @Valid @RequestBody SuggestPriorityRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        PrioritySuggestion suggestion = aiPriorityService.suggestPriority(request.cardId());
        return ResponseEntity.ok(ApiResponse.success(suggestion));
    }

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

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream AI reply token by token via SSE")
    public SseEmitter streamMessage(
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        SseEmitter emitter = new SseEmitter(120_000L); // 2 min timeout
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            try {
                Long conversationId = aiChatService.streamMessage(request, currentUser, token -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("token")
                                .data(token));
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                });

                // Send conversation ID as final event
                emitter.send(SseEmitter.event()
                        .name("done")
                        .data(conversationId));
                emitter.complete();

            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("Lỗi kết nối AI"));
                } catch (IOException ignored) {}
                emitter.completeWithError(e);
            } finally {
                executor.shutdown();
            }
        });

        return emitter;
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
