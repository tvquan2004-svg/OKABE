package com.okabe.controller;

import com.okabe.dto.request.KeyResultRequest;
import com.okabe.dto.request.ObjectiveRequest;
import com.okabe.dto.response.ApiResponse;
import com.okabe.dto.response.KeyResultResponse;
import com.okabe.dto.response.ObjectiveResponse;
import com.okabe.security.UserPrincipal;
import com.okabe.service.ObjectiveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "OKR", description = "Objectives and Key Results APIs")
public class ObjectiveController {

    private final ObjectiveService objectiveService;

    @PostMapping("/api/v1/workspaces/{workspaceId}/objectives")
    @Operation(summary = "Create a new objective")
    public ResponseEntity<ApiResponse<ObjectiveResponse>> createObjective(
            @PathVariable Long workspaceId,
            @Valid @RequestBody ObjectiveRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                objectiveService.createObjective(workspaceId, request, currentUser))); // Tạo mục tiêu (OKR) mới
    }

    @GetMapping("/api/v1/workspaces/{workspaceId}/objectives")
    @Operation(summary = "Get objectives by quarter")
    public ResponseEntity<ApiResponse<List<ObjectiveResponse>>> getObjectives(
            @PathVariable Long workspaceId,
            @RequestParam(required = false) String quarter,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                objectiveService.getObjectivesByQuarter(workspaceId, quarter, currentUser))); // Lấy danh sách mục tiêu theo quý
    }

    @GetMapping("/api/v1/workspaces/{workspaceId}/objectives/{id}")
    @Operation(summary = "Get objective detail")
    public ResponseEntity<ApiResponse<ObjectiveResponse>> getObjective(
            @PathVariable Long workspaceId,
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                objectiveService.getObjective(id, currentUser))); // Lấy chi tiết mục tiêu
    }

    @PostMapping("/api/v1/objectives/{objectiveId}/key-results")
    @Operation(summary = "Add a key result to an objective")
    public ResponseEntity<ApiResponse<KeyResultResponse>> addKeyResult(
            @PathVariable Long objectiveId,
            @Valid @RequestBody KeyResultRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                objectiveService.addKeyResult(objectiveId, request, currentUser))); // Thêm key result vào mục tiêu
    }

    @PostMapping("/api/v1/key-results/{keyResultId}/cards")
    @Operation(summary = "Link cards to a key result")
    public ResponseEntity<ApiResponse<Void>> linkCards(
            @PathVariable Long keyResultId,
            @RequestBody List<Long> cardIds,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        objectiveService.linkCardsToKeyResult(keyResultId, cardIds, currentUser); // Liên kết thẻ với key result
        return ResponseEntity.ok(ApiResponse.success(null, "Cards linked"));
    }

    @PostMapping("/api/v1/objectives/{id}/recalculate")
    @Operation(summary = "Recalculate objective progress from linked cards")
    public ResponseEntity<ApiResponse<ObjectiveResponse>> recalculateProgress(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                objectiveService.recalculateProgress(id, currentUser))); // Tính lại tiến độ mục tiêu từ thẻ liên kết
    }

    @DeleteMapping("/api/v1/objectives/{id}")
    @Operation(summary = "Delete an objective")
    public ResponseEntity<ApiResponse<Void>> deleteObjective(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        objectiveService.deleteObjective(id, currentUser); // Xoá mục tiêu
        return ResponseEntity.ok(ApiResponse.success(null, "Objective deleted"));
    }

    @GetMapping("/api/v1/workspaces/{workspaceId}/okr-tree")
    @Operation(summary = "Get full OKR tree with progress")
    public ResponseEntity<ApiResponse<List<ObjectiveResponse>>> getOkrTree(
            @PathVariable Long workspaceId,
            @RequestParam(required = false) String quarter,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                objectiveService.getOkrTree(workspaceId, quarter, currentUser))); // Lấy cây OKR đầy đủ kèm tiến độ
    }
}
