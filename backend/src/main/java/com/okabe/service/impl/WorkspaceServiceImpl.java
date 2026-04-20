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
import com.okabe.service.EmailNotificationService;
import com.okabe.service.NotificationService;
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
    private final NotificationService notificationService;
    private final EmailNotificationService emailNotificationService;

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

    // ─── Member Management ──────────────────────────────

    @Override
    public List<com.okabe.dto.response.WorkspaceMemberResponse> getWorkspaceMembers(Long workspaceId,
            UserPrincipal currentUser) {
        validateMembership(workspaceId, currentUser.getId());
        List<WorkspaceMember> members = memberRepository.findByWorkspaceId(workspaceId);
        return members.stream()
                .map(this::toMemberResponse)
                .toList();
    }

    @Override
    @Transactional
    public com.okabe.dto.response.WorkspaceMemberResponse addMemberToWorkspace(Long workspaceId,
            com.okabe.dto.request.AddWorkspaceMemberRequest request, UserPrincipal currentUser) {
        validateAdminAccess(workspaceId, currentUser.getId());
        Workspace workspace = findWorkspaceOrThrow(workspaceId);

        User userToAdd = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.email()));

        if (memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userToAdd.getId())) {
            throw new DuplicateResourceException("User is already a member of this workspace");
        }

        WorkspaceMember member = WorkspaceMember.builder()
                .workspaceId(workspaceId)
                .userId(userToAdd.getId())
                .role(request.role() != null ? request.role() : Role.MEMBER)
                .build();

        member = memberRepository.save(member);

        // Fetch user object to build full response
        member.setUser(userToAdd);

        User actor = userRepository.findById(currentUser.getId()).orElseThrow();
        notificationService.createNotification(
            userToAdd,
            actor,
            "BOARD_MEMBER_JOINED",
            "WORKSPACE",
            workspaceId,
            String.format("%s added you to the workspace", actor.getUsername())
        );

        emailNotificationService.sendInvitationEmail(
            actor,
            userToAdd,
            workspace.getName()
        );

        log.info("User {} added to workspace {} with role {}", request.email(), workspaceId, member.getRole());
        return toMemberResponse(member);
    }

    @Override
    @Transactional
    public com.okabe.dto.response.WorkspaceMemberResponse updateMemberRole(Long workspaceId, Long memberId,
            com.okabe.dto.request.UpdateMemberRoleRequest request, UserPrincipal currentUser) {
        validateAdminAccess(workspaceId, currentUser.getId());

        WorkspaceMember memberToUpdate = memberRepository.findByWorkspaceIdAndUserId(workspaceId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in workspace"));

        // If target is an OWNER, only another OWNER can change their role (or we can
        // block changing OWNER role entirely, here we enforce only OWNER can change
        // another OWNER)
        if (memberToUpdate.getRole() == Role.OWNER) {
            WorkspaceMember currentMember = memberRepository
                    .findByWorkspaceIdAndUserId(workspaceId, currentUser.getId())
                    .orElseThrow(() -> new UnauthorizedException("Not a member"));
            if (currentMember.getRole() != Role.OWNER) {
                throw new UnauthorizedException("Only OWNER can change another OWNER's role");
            }
        }

        memberToUpdate.setRole(request.role());
        memberToUpdate = memberRepository.save(memberToUpdate);

        log.info("User {} role updated to {} in workspace {}", memberId, request.role(), workspaceId);
        return toMemberResponse(memberToUpdate);
    }

    @Override
    @Transactional
    public void removeMemberFromWorkspace(Long workspaceId, Long memberId, UserPrincipal currentUser) {
        validateAdminAccess(workspaceId, currentUser.getId());

        WorkspaceMember memberToRemove = memberRepository.findByWorkspaceIdAndUserId(workspaceId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in workspace"));

        // Cannot remove an OWNER unless you are an OWNER
        if (memberToRemove.getRole() == Role.OWNER) {
            WorkspaceMember currentMember = memberRepository
                    .findByWorkspaceIdAndUserId(workspaceId, currentUser.getId())
                    .orElseThrow(() -> new UnauthorizedException("Not a member"));
            if (currentMember.getRole() != Role.OWNER) {
                throw new UnauthorizedException("Only OWNER can remove another OWNER");
            }
        }

        memberRepository.delete(memberToRemove);
        log.info("User {} removed from workspace {}", memberId, workspaceId);
    }

    private com.okabe.dto.response.WorkspaceMemberResponse toMemberResponse(WorkspaceMember member) {
        User user = member.getUser();
        if (user == null) {
            user = userRepository.findById(member.getUserId()).orElse(new User());
        }
        return com.okabe.dto.response.WorkspaceMemberResponse.builder()
                .userId(member.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .role(member.getRole().name())
                .joinedAt(member.getJoinedAt())
                .build();
    }

    // ─── Helper Methods ─────────────────────────────────

    private Workspace findWorkspaceOrThrow(Long id) {
        return workspaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", id));
    }

    private void validateMembership(Long workspaceId, Long userId) {
        Workspace workspace = findWorkspaceOrThrow(workspaceId);
        // Owner always has access
        if (workspace.getOwner().getId().equals(userId)) {
            return;
        }

        if (!memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new UnauthorizedException("You are not a member of this workspace");
        }
    }

    private void validateAdminAccess(Long workspaceId, Long userId) {
        Workspace workspace = findWorkspaceOrThrow(workspaceId);
        // Owner always has access
        if (workspace.getOwner().getId().equals(userId)) {
            return;
        }

        boolean hasAccess = memberRepository.existsByWorkspaceIdAndUserIdAndRoleIn(
                workspaceId, userId, List.of(Role.OWNER, Role.ADMIN));
        if (!hasAccess) {
            throw new UnauthorizedException("Only OWNER or ADMIN can perform this action");
        }
    }

    private WorkspaceResponse toResponse(Workspace workspace, Long currentUserId) {
        List<WorkspaceMember> members = memberRepository.findByWorkspaceId(workspace.getId());
        User owner = workspace.getOwner();
        
        // Find user's role in the list we already fetched
        String currentRole = members.stream()
                .filter(m -> m.getUserId().equals(currentUserId))
                .map(m -> m.getRole().name())
                .findFirst()
                .orElse(null);

        // Fallback for owner if not found in members list
        if (currentRole == null && owner.getId().equals(currentUserId)) {
            currentRole = Role.OWNER.name();
        }

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
                WHITESPACE.matcher(normalized).replaceAll("-")).replaceAll("").toLowerCase(Locale.ENGLISH);
        return slug.length() > 100 ? slug.substring(0, 100) : slug;
    }
}
