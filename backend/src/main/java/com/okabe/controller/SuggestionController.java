package com.okabe.controller;

import com.okabe.dto.response.ApiResponse;
import com.okabe.dto.response.SuggestionResponse;
import com.okabe.security.UserPrincipal;
import com.okabe.service.impl.SmartSuggestionServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Suggestion", description = "Smart suggestions APIs")
public class SuggestionController {

    private final SmartSuggestionServiceImpl suggestionService;

    @GetMapping("/api/v1/workspaces/{workspaceId}/suggestions")
    @Operation(summary = "Get proactive suggestions for a workspace")
    public ResponseEntity<ApiResponse<List<SuggestionResponse>>> getSuggestions(
            @PathVariable Long workspaceId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<SuggestionResponse> suggestions = suggestionService.getSuggestions(workspaceId, currentUser); // Lấy gợi ý thông minh cho workspace
        return ResponseEntity.ok(ApiResponse.success(suggestions));
    }

    @PostMapping("/api/v1/suggestions/dismiss")
    @Operation(summary = "Dismiss a suggestion")
    public ResponseEntity<ApiResponse<Void>> dismissSuggestion(
            @RequestParam String type,
            @RequestParam(required = false) Long cardId,
            @RequestParam Long workspaceId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        suggestionService.dismissSuggestion(type, cardId, workspaceId, currentUser); // Tắt gợi ý
        return ResponseEntity.ok(ApiResponse.success(null, "Suggestion dismissed"));
    }
}
