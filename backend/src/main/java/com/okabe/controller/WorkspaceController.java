package com.okabe.controller;

import com.okabe.dto.request.CreateWorkspaceRequest;
import com.okabe.dto.request.UpdateWorkspaceRequest;
import com.okabe.dto.response.ApiResponse;
import com.okabe.dto.response.WorkspaceResponse;
import com.okabe.security.UserPrincipal;
import com.okabe.service.WorkspaceService;
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
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
@Tag(name = "Workspace", description = "Workspace management APIs")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @GetMapping
    @Operation(summary = "Get all workspaces for the current user")
    public ResponseEntity<ApiResponse<List<WorkspaceResponse>>> getUserWorkspaces(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<WorkspaceResponse> workspaces = workspaceService.getUserWorkspaces(currentUser);
        return ResponseEntity.ok(ApiResponse.success(workspaces));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get workspace by ID")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> getWorkspace(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        WorkspaceResponse workspace = workspaceService.getWorkspace(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(workspace));
    }

    @PostMapping
    @Operation(summary = "Create a new workspace")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> createWorkspace(
            @Valid @RequestBody CreateWorkspaceRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        WorkspaceResponse workspace = workspaceService.createWorkspace(request, currentUser);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(workspace, "Workspace created successfully"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a workspace")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> updateWorkspace(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWorkspaceRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        WorkspaceResponse workspace = workspaceService.updateWorkspace(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(workspace, "Workspace updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a workspace (OWNER only)")
    public ResponseEntity<ApiResponse<Void>> deleteWorkspace(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        workspaceService.deleteWorkspace(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(null, "Workspace deleted successfully"));
    }

    // ─── Member Management Endpoints ──────────────────────────────

    @GetMapping("/{id}/members")
    @Operation(summary = "Get workspace members")
    public ResponseEntity<ApiResponse<List<com.okabe.dto.response.WorkspaceMemberResponse>>> getWorkspaceMembers(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(workspaceService.getWorkspaceMembers(id, currentUser)));
    }

    @PostMapping("/{id}/members")
    @Operation(summary = "Invite a member to workspace (ADMIN/OWNER only)")
    public ResponseEntity<ApiResponse<Void>> addMemberToWorkspace(
            @PathVariable Long id,
            @Valid @RequestBody com.okabe.dto.request.AddWorkspaceMemberRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        workspaceService.inviteMember(id, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(null, "Invitation sent successfully"));
    }

    @PutMapping("/{id}/members/{memberId}/role")
    @Operation(summary = "Update member role (ADMIN/OWNER only)")
    public ResponseEntity<ApiResponse<com.okabe.dto.response.WorkspaceMemberResponse>> updateMemberRole(
            @PathVariable Long id,
            @PathVariable Long memberId,
            @Valid @RequestBody com.okabe.dto.request.UpdateMemberRoleRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(workspaceService.updateMemberRole(id, memberId, request, currentUser), "Role updated successfully"));
    }

    @DeleteMapping("/{id}/members/{memberId}")
    @Operation(summary = "Remove member from workspace (ADMIN/OWNER only)")
    public ResponseEntity<ApiResponse<Void>> removeMemberFromWorkspace(
            @PathVariable Long id,
            @PathVariable Long memberId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        workspaceService.removeMemberFromWorkspace(id, memberId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(null, "Member removed successfully"));
    }

    @PostMapping("/invitations/accept")
    @Operation(summary = "Accept a workspace invitation")
    public ResponseEntity<ApiResponse<Void>> acceptInvitation(
            @RequestParam String token,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        workspaceService.acceptInvitation(token, currentUser);
        return ResponseEntity.ok(ApiResponse.success(null, "Invitation accepted"));
    }

    @PostMapping("/invitations/reject")
    @Operation(summary = "Reject a workspace invitation")
    public ResponseEntity<ApiResponse<Void>> rejectInvitation(
            @RequestParam String token,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        workspaceService.rejectInvitation(token, currentUser);
        return ResponseEntity.ok(ApiResponse.success(null, "Invitation rejected"));
    }
}
