package com.okabe.service;

import com.okabe.dto.request.CreateWorkspaceRequest;
import com.okabe.dto.request.UpdateWorkspaceRequest;
import com.okabe.dto.response.WorkspaceResponse;
import com.okabe.security.UserPrincipal;

import java.util.List;

public interface WorkspaceService {

    /**
     * Get all workspaces where the current user is a member.
     */
    List<WorkspaceResponse> getUserWorkspaces(UserPrincipal currentUser);

    /**
     * Get a workspace by ID. User must be a member.
     */
    WorkspaceResponse getWorkspace(Long workspaceId, UserPrincipal currentUser);

    /**
     * Create a new workspace. Creator becomes OWNER.
     */
    WorkspaceResponse createWorkspace(CreateWorkspaceRequest request, UserPrincipal currentUser);

    /**
     * Update workspace name/description. Only OWNER or ADMIN can update.
     */
    WorkspaceResponse updateWorkspace(Long workspaceId, UpdateWorkspaceRequest request, UserPrincipal currentUser);

    /**
     * Delete a workspace. Only OWNER can delete.
     */
    void deleteWorkspace(Long workspaceId, UserPrincipal currentUser);

    // Member Management
    List<com.okabe.dto.response.WorkspaceMemberResponse> getWorkspaceMembers(Long workspaceId, UserPrincipal currentUser);
    com.okabe.dto.response.WorkspaceMemberResponse addMemberToWorkspace(Long workspaceId, com.okabe.dto.request.AddWorkspaceMemberRequest request, UserPrincipal currentUser);
    com.okabe.dto.response.WorkspaceMemberResponse updateMemberRole(Long workspaceId, Long memberId, com.okabe.dto.request.UpdateMemberRoleRequest request, UserPrincipal currentUser);
    void removeMemberFromWorkspace(Long workspaceId, Long memberId, UserPrincipal currentUser);

    // Invitation Management
    void inviteMember(Long workspaceId, com.okabe.dto.request.AddWorkspaceMemberRequest request, UserPrincipal currentUser);
    void acceptInvitation(String token, UserPrincipal currentUser);
    void rejectInvitation(String token, UserPrincipal currentUser);
}
