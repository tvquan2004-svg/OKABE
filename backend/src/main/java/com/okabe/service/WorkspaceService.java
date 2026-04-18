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
}
