package com.okabe.service.impl;

import com.okabe.dto.request.CreateWorkspaceRequest;
import com.okabe.dto.request.UpdateWorkspaceRequest;
import com.okabe.dto.response.WorkspaceResponse;
import com.okabe.entity.User;
import com.okabe.entity.Workspace;
import com.okabe.entity.WorkspaceMember;
import com.okabe.entity.enums.Role;
import com.okabe.exception.DuplicateResourceException;
import com.okabe.exception.ResourceNotFoundException;
import com.okabe.exception.UnauthorizedException;
import com.okabe.repository.UserRepository;
import com.okabe.repository.WorkspaceMemberRepository;
import com.okabe.repository.WorkspaceRepository;
import com.okabe.security.UserPrincipal;
import com.okabe.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkspaceServiceImpl implements WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final UserRepository userRepository;

    @Override
    public List<WorkspaceResponse> getUserWorkspaces(UserPrincipal currentUser) {
        List<Workspace> workspaces = workspaceRepository.findAllByMemberUserId(currentUser.getId());
        return workspaces.stream()
                .map(ws -> toResponse(ws, currentUser.getId()))
                .toList();
    }

    @Override
    public WorkspaceResponse getWorkspace(Long workspaceId, UserPrincipal currentUser) {
        Workspace workspace = findWorkspaceOrThrow(workspaceId);
        validateMembership(workspaceId, currentUser.getId());
        return toResponse(workspace, currentUser.getId());
    }

    @Override
    @Transactional
    public WorkspaceResponse createWorkspace(CreateWorkspaceRequest request, UserPrincipal currentUser) {
        String slug = generateSlug(request.name());

        if (workspaceRepository.existsBySlug(slug)) {
            slug = slug + "-" + System.currentTimeMillis();
        }

        User owner = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUser.getId()));

        Workspace workspace = Workspace.builder()
                .name(request.name())
                .slug(slug)
                .description(request.description())
                .owner(owner)
                .build();

        workspace = workspaceRepository.save(workspace);

        // Add creator as OWNER member
        WorkspaceMember ownerMember = WorkspaceMember.builder()
                .workspaceId(workspace.getId())
                .userId(currentUser.getId())
                .role(Role.OWNER)
                .build();
        memberRepository.save(ownerMember);

        log.info("Workspace created: {} by user {}", workspace.getName(), currentUser.getEmail());
        return toResponse(workspace, currentUser.getId());
    }

    @Override
    @Transactional
    public WorkspaceResponse updateWorkspace(Long workspaceId, UpdateWorkspaceRequest request,
                                             UserPrincipal currentUser) {
        Workspace workspace = findWorkspaceOrThrow(workspaceId);
        validateAdminAccess(workspaceId, currentUser.getId());

        if (request.name() != null && !request.name().isBlank()) {
            workspace.setName(request.name());
        }
        if (request.description() != null) {
            workspace.setDescription(request.description());
        }

        workspace = workspaceRepository.save(workspace);
        log.info("Workspace updated: {} by user {}", workspace.getName(), currentUser.getEmail());
        return toResponse(workspace, currentUser.getId());
    }

    @Override
    @Transactional
    public void deleteWorkspace(Long workspaceId, UserPrincipal currentUser) {
        Workspace workspace = findWorkspaceOrThrow(workspaceId);

        if (!workspace.getOwner().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("Only the workspace owner can delete it");
        }

        workspaceRepository.delete(workspace);
        log.info("Workspace deleted: {} by user {}", workspace.getName(), currentUser.getEmail());
    }

    // ─── Helper Methods ─────────────────────────────────

    private Workspace findWorkspaceOrThrow(Long id) {
        return workspaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", id));
    }

    private void validateMembership(Long workspaceId, Long userId) {
        if (!memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new UnauthorizedException("You are not a member of this workspace");
        }
    }

    private void validateAdminAccess(Long workspaceId, Long userId) {
        boolean hasAccess = memberRepository.existsByWorkspaceIdAndUserIdAndRoleIn(
                workspaceId, userId, List.of(Role.OWNER, Role.ADMIN));
        if (!hasAccess) {
            throw new UnauthorizedException("Only OWNER or ADMIN can perform this action");
        }
    }

    private WorkspaceResponse toResponse(Workspace workspace, Long currentUserId) {
        List<WorkspaceMember> members = memberRepository.findByWorkspaceId(workspace.getId());
        String currentRole = members.stream()
                .filter(m -> m.getUserId().equals(currentUserId))
                .map(m -> m.getRole().name())
                .findFirst()
                .orElse(null);

        User owner = workspace.getOwner();

        return WorkspaceResponse.builder()
                .id(workspace.getId())
                .name(workspace.getName())
                .slug(workspace.getSlug())
                .description(workspace.getDescription())
                .owner(WorkspaceResponse.OwnerInfo.builder()
                        .id(owner.getId())
                        .username(owner.getUsername())
                        .email(owner.getEmail())
                        .avatarUrl(owner.getAvatarUrl())
                        .build())
                .currentUserRole(currentRole)
                .memberCount(members.size())
                .createdAt(workspace.getCreatedAt())
                .build();
    }

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");

    private String generateSlug(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String slug = NON_LATIN.matcher(
                WHITESPACE.matcher(normalized).replaceAll("-")
        ).replaceAll("").toLowerCase(Locale.ENGLISH);
        return slug.length() > 100 ? slug.substring(0, 100) : slug;
    }
}
