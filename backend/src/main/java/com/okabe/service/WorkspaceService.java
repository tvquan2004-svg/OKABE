package com.okabe.service;

import com.okabe.dto.request.CreateWorkspaceRequest;
import com.okabe.dto.request.UpdateWorkspaceRequest;
import com.okabe.dto.response.WorkspaceResponse;
import com.okabe.security.UserPrincipal;

import java.util.List;

public interface WorkspaceService {

    // Lấy tất cả workspace mà user là thành viên
    List<WorkspaceResponse> getUserWorkspaces(UserPrincipal currentUser);

    // Lấy thông tin workspace theo id (user phải là thành viên)
    WorkspaceResponse getWorkspace(Long workspaceId, UserPrincipal currentUser);

    // Tạo workspace mới (người tạo trở thành OWNER)
    WorkspaceResponse createWorkspace(CreateWorkspaceRequest request, UserPrincipal currentUser);

    // Cập nhật tên/mô tả workspace (chỉ OWNER hoặc ADMIN)
    WorkspaceResponse updateWorkspace(Long workspaceId, UpdateWorkspaceRequest request, UserPrincipal currentUser);

    // Xoá workspace (chỉ OWNER)
    void deleteWorkspace(Long workspaceId, UserPrincipal currentUser);

    // Quản lý thành viên
    List<com.okabe.dto.response.WorkspaceMemberResponse> getWorkspaceMembers(Long workspaceId, UserPrincipal currentUser);
    com.okabe.dto.response.WorkspaceMemberResponse addMemberToWorkspace(Long workspaceId, com.okabe.dto.request.AddWorkspaceMemberRequest request, UserPrincipal currentUser);
    com.okabe.dto.response.WorkspaceMemberResponse updateMemberRole(Long workspaceId, Long memberId, com.okabe.dto.request.UpdateMemberRoleRequest request, UserPrincipal currentUser);
    void removeMemberFromWorkspace(Long workspaceId, Long memberId, UserPrincipal currentUser);

    // Quản lý lời mời
    void inviteMember(Long workspaceId, com.okabe.dto.request.AddWorkspaceMemberRequest request, UserPrincipal currentUser);
    void acceptInvitation(String token, UserPrincipal currentUser);
    void acceptInvitationById(Long invitationId, UserPrincipal currentUser);
    void rejectInvitation(String token, UserPrincipal currentUser);
    void rejectInvitationById(Long invitationId, UserPrincipal currentUser);
}
