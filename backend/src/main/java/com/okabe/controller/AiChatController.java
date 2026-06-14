package com.okabe.controller;

import com.okabe.dto.request.BreakdownRequest;
import com.okabe.dto.request.ChatRequest;
import com.okabe.dto.request.SentimentRequest;
import com.okabe.dto.request.SuggestPriorityRequest;
import com.okabe.dto.response.ApiResponse;
import com.okabe.dto.response.ChatResponse;
import com.okabe.dto.response.ConversationResponse;
import com.okabe.dto.response.MessageResponse;
import com.okabe.dto.response.PrioritySuggestion;
import com.okabe.dto.response.SentimentResult;
import com.okabe.dto.response.StandupSummary;
import com.okabe.dto.response.SubtaskSuggestion;
import com.okabe.security.UserPrincipal;
import com.okabe.service.AiChatService;
import com.okabe.service.AiPriorityService;
import com.okabe.service.AiSentimentService;
import com.okabe.service.AiStandupService;
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
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Executor;


@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI Chat", description = "AI Assistant chatbox APIs")
public class AiChatController {

    private final AiChatService aiChatService;
    private final AiTaskBreakdownService aiTaskBreakdownService;
    private final AiPriorityService aiPriorityService;
    private final AiStandupService aiStandupService;
    private final AiSentimentService aiSentimentService;
    private final Executor taskExecutor;

    @PostMapping("/breakdown")
    @Operation(summary = "Phân rã task thành các subtask bằng AI")
    public ResponseEntity<ApiResponse<List<SubtaskSuggestion>>> breakdownTask(
            @Valid @RequestBody BreakdownRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<SubtaskSuggestion> suggestions = aiTaskBreakdownService.breakdownTask(request.cardId()); // Phân rã task thành subtask bằng AI
        return ResponseEntity.ok(ApiResponse.success(suggestions));
    }

    @PostMapping("/suggest-priority")
    @Operation(summary = "Gợi ý độ ưu tiên cho card dựa trên context")
    public ResponseEntity<ApiResponse<PrioritySuggestion>> suggestPriority(
            @Valid @RequestBody SuggestPriorityRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        PrioritySuggestion suggestion = aiPriorityService.suggestPriority(request.cardId()); // AI gợi ý độ ưu tiên dựa trên ngữ cảnh
        return ResponseEntity.ok(ApiResponse.success(suggestion));
    }

    @GetMapping("/standup")
    @Operation(summary = "Tạo standup summary cho một user trong ngày")
    public ResponseEntity<ApiResponse<StandupSummary>> getStandup(
            @RequestParam Long workspaceId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String date,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Long targetUserId = userId != null ? userId : currentUser.getId(); // Mặc định là user hiện tại
        LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now(); // Mặc định là hôm nay
        StandupSummary summary = aiStandupService.generateStandup(targetUserId, workspaceId, targetDate); // AI tạo standup summary
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @PostMapping("/sentiment")
    @Operation(summary = "Phân tích cảm xúc của văn bản")
    public ResponseEntity<ApiResponse<SentimentResult>> analyzeSentiment(
            @Valid @RequestBody SentimentRequest request) {
        SentimentResult result = aiSentimentService.analyzeSentiment(request.text()); // Phân tích cảm xúc văn bản
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/standup/workspace/{workspaceId}")
    @Operation(summary = "Tổng hợp standup của tất cả member trong workspace")
    public ResponseEntity<ApiResponse<List<StandupSummary>>> getWorkspaceStandup(
            @PathVariable Long workspaceId,
            @RequestParam(required = false) String date,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now(); // Mặc định là hôm nay
        List<StandupSummary> summaries = aiStandupService.generateWorkspaceStandup(workspaceId, targetDate); // Tổng hợp standup toàn workspace
        return ResponseEntity.ok(ApiResponse.success(summaries));
    }

    @PostMapping("/conversations")
    @Operation(summary = "Tạo cuộc hội thoại mới")
    public ResponseEntity<ApiResponse<ConversationResponse>> createConversation(
            @RequestParam(required = false) Long boardId,
            @RequestParam(required = false) Long workspaceId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        aiChatService.createConversation(boardId, workspaceId, currentUser), // Tạo cuộc hội thoại mới với AI
                        "Đã tạo cuộc hội thoại mới"));
    }

    @GetMapping("/conversations")
    @Operation(summary = "Lấy danh sách cuộc hội thoại của user")
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> getConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                aiChatService.getConversations(page, size, currentUser))); // Lấy danh sách cuộc hội thoại
    }

    @GetMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "Lấy tất cả tin nhắn trong một cuộc hội thoại")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getMessages(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                aiChatService.getMessages(conversationId, currentUser))); // Lấy tin nhắn trong cuộc hội thoại
    }

    @PostMapping("/chat")
    @Operation(summary = "Gửi tin nhắn đến AI và nhận phản hồi")
    public ResponseEntity<ApiResponse<ChatResponse>> sendMessage(
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                aiChatService.sendMessage(request, currentUser))); // Gửi tin nhắn đến AI và nhận phản hồi
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream AI reply token by token via SSE")
    public SseEmitter streamMessage(
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        SseEmitter emitter = new SseEmitter(120_000L); // Tạo SSE emitter với timeout 2 phút

        try {
            taskExecutor.execute(() -> {
                try {
                    Long conversationId = aiChatService.streamMessage(request, currentUser, token -> { // Stream AI trả lời token-by-token
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("token") // Gửi từng token qua SSE
                                    .data(token));
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    });

                    // Gửi conversation ID khi kết thúc
                    emitter.send(SseEmitter.event()
                            .name("done")
                            .data(conversationId));
                    emitter.complete();

                } catch (Exception e) {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("error")
                                .data("Lỗi kết nối AI")); // Thông báo lỗi cho client
                    } catch (IOException ignored) {}
                    emitter.completeWithError(e);
                }
            });
        } catch (Exception e) {
            log.warn("SSE task rejected, executor queue full: {}", e.getMessage());
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("Hệ thống đang quá tải, vui lòng thử lại sau"));
            } catch (IOException ignored) {}
            emitter.completeWithError(new RuntimeException("Hệ thống đang quá tải"));
        }

        return emitter;
    }

    @DeleteMapping("/conversations/{conversationId}")
    @Operation(summary = "Xoá một cuộc hội thoại")
    public ResponseEntity<ApiResponse<Void>> deleteConversation(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        aiChatService.deleteConversation(conversationId, currentUser); // Xoá cuộc hội thoại
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xoá cuộc hội thoại"));
    }
}
