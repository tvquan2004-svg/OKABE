package com.okabe.controller;

import com.okabe.dto.request.CreateBoardRequest;
import com.okabe.dto.request.UpdateBoardRequest;
import com.okabe.dto.response.ApiResponse;
import com.okabe.dto.response.BoardResponse;
import com.okabe.security.UserPrincipal;
import com.okabe.service.BoardService;
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
@Tag(name = "Board", description = "Board management APIs")
public class BoardController {

    private final BoardService boardService;

    @GetMapping("/api/v1/workspaces/{workspaceId}/boards")
    @Operation(summary = "Get all boards in a workspace")
    public ResponseEntity<ApiResponse<List<BoardResponse>>> getBoardsByWorkspace(
            @PathVariable Long workspaceId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                boardService.getBoardsByWorkspace(workspaceId, currentUser)));
    }

    @GetMapping("/api/v1/boards/{id}")
    @Operation(summary = "Get full board with lists and cards")
    public ResponseEntity<ApiResponse<BoardResponse>> getBoard(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(boardService.getBoard(id, currentUser)));
    }

    @PostMapping("/api/v1/workspaces/{workspaceId}/boards")
    @Operation(summary = "Create a new board")
    public ResponseEntity<ApiResponse<BoardResponse>> createBoard(
            @PathVariable Long workspaceId,
            @Valid @RequestBody CreateBoardRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(boardService.createBoard(workspaceId, request, currentUser),
                        "Board created successfully"));
    }

    @PutMapping("/api/v1/boards/{id}")
    @Operation(summary = "Update a board")
    public ResponseEntity<ApiResponse<BoardResponse>> updateBoard(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBoardRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                boardService.updateBoard(id, request, currentUser), "Board updated"));
    }

    @DeleteMapping("/api/v1/boards/{id}")
    @Operation(summary = "Delete a board")
    public ResponseEntity<ApiResponse<Void>> deleteBoard(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        boardService.deleteBoard(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(null, "Board deleted"));
    }
}
