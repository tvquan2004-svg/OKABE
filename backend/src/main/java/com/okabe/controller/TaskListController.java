package com.okabe.controller;

import com.okabe.dto.request.CreateListRequest;
import com.okabe.dto.request.ReorderListRequest;
import com.okabe.dto.request.UpdateListRequest;
import com.okabe.dto.response.ApiResponse;
import com.okabe.dto.response.ListResponse;
import com.okabe.security.UserPrincipal;
import com.okabe.service.TaskListService;
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
@RequiredArgsConstructor
@Tag(name = "List", description = "Task list management APIs")
public class TaskListController {

    private final TaskListService taskListService;

    @GetMapping("/api/v1/boards/{boardId}/lists")
    @Operation(summary = "Get all lists in a board")
    public ResponseEntity<ApiResponse<List<ListResponse>>> getListsByBoard(
            @PathVariable Long boardId,
            @RequestParam(required = false, defaultValue = "false") boolean archived,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<ListResponse> lists = archived 
                ? taskListService.getArchivedLists(boardId, currentUser)
                : taskListService.getListsByBoard(boardId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(lists));
    }

    @PostMapping("/api/v1/boards/{boardId}/lists")
    @Operation(summary = "Create a new list in a board")
    public ResponseEntity<ApiResponse<ListResponse>> createList(
            @PathVariable Long boardId,
            @Valid @RequestBody CreateListRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(taskListService.createList(boardId, request, currentUser),
                        "List created"));
    }

    @PutMapping("/api/v1/lists/{id}")
    @Operation(summary = "Update a list")
    public ResponseEntity<ApiResponse<ListResponse>> updateList(
            @PathVariable Long id,
            @Valid @RequestBody UpdateListRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                taskListService.updateList(id, request, currentUser)));
    }

    @PutMapping("/api/v1/boards/{boardId}/lists/reorder")
    @Operation(summary = "Reorder lists in a board")
    public ResponseEntity<ApiResponse<Void>> reorderLists(
            @PathVariable Long boardId,
            @Valid @RequestBody ReorderListRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        taskListService.reorderLists(boardId, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(null, "Lists reordered"));
    }

    @DeleteMapping("/api/v1/lists/{id}")
    @Operation(summary = "Delete a list")
    public ResponseEntity<ApiResponse<Void>> deleteList(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        taskListService.deleteList(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(null, "List deleted"));
    }

    @PutMapping("/api/v1/lists/{id}/archive")
    @Operation(summary = "Archive a list")
    public ResponseEntity<ApiResponse<ListResponse>> archiveList(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                taskListService.archiveList(id, currentUser), "List archived"));
    }

    @PutMapping("/api/v1/lists/{id}/restore")
    @Operation(summary = "Restore a list")
    public ResponseEntity<ApiResponse<ListResponse>> restoreList(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                taskListService.restoreList(id, currentUser), "List restored"));
    }
}
