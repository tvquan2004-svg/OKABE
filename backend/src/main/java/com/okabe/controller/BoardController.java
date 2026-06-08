package com.okabe.controller;

import com.okabe.dto.request.CreateBoardRequest;
import com.okabe.dto.request.ReorderBoardRequest;
import com.okabe.dto.request.UpdateBoardRequest;
import com.okabe.dto.response.ActivityResponse;
import com.okabe.dto.response.ApiResponse;
import com.okabe.dto.response.BoardResponse;
import com.okabe.security.UserPrincipal;
import com.okabe.service.ActivityService;
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
    private final ActivityService activityService;

    @GetMapping("/api/v1/workspaces/{workspaceId}/boards")
    @Operation(summary = "Get all boards in a workspace")
    public ResponseEntity<ApiResponse<List<BoardResponse>>> getBoardsByWorkspace(
            @PathVariable Long workspaceId,
            @RequestParam(required = false, defaultValue = "false") boolean archived,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<BoardResponse> boards = archived 
                ? boardService.getArchivedBoards(workspaceId, currentUser) // Lấy danh sách bảng đã ẩn
                : boardService.getBoardsByWorkspace(workspaceId, currentUser); // Lấy danh sách bảng đang hoạt động
        return ResponseEntity.ok(ApiResponse.success(boards));
    }

    @GetMapping("/api/v1/boards/{id}")
    @Operation(summary = "Get full board with lists and cards")
    public ResponseEntity<ApiResponse<BoardResponse>> getBoard(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(boardService.getBoard(id, currentUser))); // Lấy chi tiết bảng kèm danh sách và thẻ
    }

    @PostMapping("/api/v1/workspaces/{workspaceId}/boards")
    @Operation(summary = "Create a new board")
    public ResponseEntity<ApiResponse<BoardResponse>> createBoard(
            @PathVariable Long workspaceId,
            @Valid @RequestBody CreateBoardRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED) // Trả về HTTP 201
                .body(ApiResponse.success(boardService.createBoard(workspaceId, request, currentUser), // Tạo bảng mới
                        "Board created successfully"));
    }

    @PutMapping("/api/v1/boards/{id}")
    @Operation(summary = "Update a board")
    public ResponseEntity<ApiResponse<BoardResponse>> updateBoard(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBoardRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                boardService.updateBoard(id, request, currentUser), "Board updated")); // Cập nhật thông tin bảng
    }

    @PutMapping("/api/v1/workspaces/{workspaceId}/boards/reorder")
    @Operation(summary = "Reorder boards in a workspace")
    public ResponseEntity<ApiResponse<Void>> reorderBoards(
            @PathVariable Long workspaceId,
            @Valid @RequestBody ReorderBoardRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        boardService.reorderBoards(workspaceId, request, currentUser); // Sắp xếp lại thứ tự bảng
        return ResponseEntity.ok(ApiResponse.success(null, "Boards reordered"));
    }

    @DeleteMapping("/api/v1/boards/{id}")
    @Operation(summary = "Delete a board")
    public ResponseEntity<ApiResponse<Void>> deleteBoard(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        boardService.deleteBoard(id, currentUser); // Xoá bảng vĩnh viễn
        return ResponseEntity.ok(ApiResponse.success(null, "Board deleted"));
    }

    @PatchMapping(value = "/api/v1/boards/{id}/background", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update board background")
    public ResponseEntity<ApiResponse<BoardResponse>> updateBackground(
            @PathVariable Long id,
            @RequestParam("type") String type,
            @RequestParam(value = "value", required = false) String value,
            @RequestParam(value = "file", required = false) org.springframework.web.multipart.MultipartFile file,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        System.out.println(">>> RECEIVED BACKGROUND UPDATE REQUEST for board: " + id + ", type: " + type);
        if (file != null) {
            System.out.println(">>> FILE RECEIVED: " + file.getOriginalFilename() + " (" + file.getSize() + " bytes)");
        }
        
        return ResponseEntity.ok(ApiResponse.success(
                boardService.updateBackground(id, type, value, file, currentUser), // Cập nhật ảnh nền bảng
                "Background updated successfully"
        ));
    }

    @PostMapping("/api/v1/boards/{id}/invite")
    @Operation(summary = "Invite a member to a board")
    public ResponseEntity<ApiResponse<Void>> inviteMember(
            @PathVariable Long id,
            @RequestParam String email,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        boardService.inviteMember(id, email, currentUser); // Mời thành viên vào bảng
        return ResponseEntity.ok(ApiResponse.success(null, "Invitation sent"));
    }

    @PutMapping("/api/v1/boards/{id}/archive")
    @Operation(summary = "Archive a board")
    public ResponseEntity<ApiResponse<BoardResponse>> archiveBoard(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                boardService.archiveBoard(id, currentUser), "Board archived")); // Ẩn bảng
    }

    @PutMapping("/api/v1/boards/{id}/restore")
    @Operation(summary = "Restore a board")
    public ResponseEntity<ApiResponse<BoardResponse>> restoreBoard(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                boardService.restoreBoard(id, currentUser), "Board restored")); // Khôi phục bảng từ trạng thái ẩn
    }

    @GetMapping("/api/v1/boards/{id}/activities")
    @Operation(summary = "Get all activities for a board")
    public ResponseEntity<ApiResponse<List<ActivityResponse>>> getBoardActivities(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(activityService.getBoardActivities(id))); // Lấy lịch sử hoạt động của bảng
    }
}
