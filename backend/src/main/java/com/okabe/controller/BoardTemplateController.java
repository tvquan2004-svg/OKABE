package com.okabe.controller;

import com.okabe.dto.request.SaveAsTemplateRequest;
import com.okabe.dto.response.ApiResponse;
import com.okabe.dto.response.BoardTemplateResponse;
import com.okabe.security.UserPrincipal;
import com.okabe.service.BoardTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
@Tag(name = "Board Template", description = "Board template management APIs")
public class BoardTemplateController {

    private final BoardTemplateService templateService;

    @GetMapping
    @Operation(summary = "Get all available templates")
    public ResponseEntity<ApiResponse<List<BoardTemplateResponse>>> getAllTemplates(
            @RequestParam(required = false) Long workspaceId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(templateService.getAllTemplates(workspaceId, currentUser)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get template details")
    public ResponseEntity<ApiResponse<BoardTemplateResponse>> getTemplate(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(templateService.getTemplate(id, currentUser)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a template")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        templateService.deleteTemplate(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(null, "Template deleted successfully"));
    }

    @PostMapping("/boards/{boardId}/save")
    @Operation(summary = "Save an existing board as a template")
    public ResponseEntity<ApiResponse<BoardTemplateResponse>> saveAsTemplate(
            @PathVariable Long boardId,
            @Valid @RequestBody SaveAsTemplateRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(templateService.saveAsTemplate(boardId, request, currentUser)));
    }
}
