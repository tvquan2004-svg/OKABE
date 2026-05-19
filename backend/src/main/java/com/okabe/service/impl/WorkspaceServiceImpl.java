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
import com.okabe.repository.BoardRepository;
import com.okabe.repository.UserRepository;
import com.okabe.repository.WorkspaceMemberRepository;
import com.okabe.repository.WorkspaceRepository;
import com.okabe.security.UserPrincipal;
import com.okabe.entity.WorkspaceInvitation;
import com.okabe.repository.WorkspaceInvitationRepository;
import com.okabe.service.WorkspaceService;
import com.okabe.service.EmailNotificationService;
import com.okabe.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkspaceServiceImpl implements WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final WorkspaceInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final BoardRepository boardRepository;
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
        // Keep this method for direct addition if needed (e.g. by owner)
        // But for common flow, we use inviteMember
        return inviteAndAddDirectly(workspaceId, request, currentUser);
    }

    private com.okabe.dto.response.WorkspaceMemberResponse inviteAndAddDirectly(Long workspaceId,
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
        member.setUser(userToAdd);

        User actor = userRepository.findById(currentUser.getId()).orElseThrow();
        notificationService.createNotification(
            userToAdd,
            actor,
            "WORKSPACE_JOINED",
            "WORKSPACE",
            workspaceId,
            workspaceId,
            String.format("%s đã thêm bạn vào không gian làm việc: %s", actor.getUsername(), workspace.getName())
        );

        emailNotificationService.sendWorkspaceAddedEmail(
            actor,
            userToAdd,
            workspace.getName(),
            workspaceId
        );

        log.info("User {} added directly to workspace {} by {}", request.email(), workspaceId, currentUser.getEmail());
        return toMemberResponse(member);
    }

    @Override
    @Transactional
    public void inviteMember(Long workspaceId, com.okabe.dto.request.AddWorkspaceMemberRequest request, UserPrincipal currentUser) {
        log.info("[SERVICE] inviteMember triggered for workspace: {}, email: {}", workspaceId, request.email());
        validateAdminAccess(workspaceId, currentUser.getId());
        Workspace workspace = findWorkspaceOrThrow(workspaceId);

        if (memberRepository.existsByWorkspaceIdAndEmail(workspaceId, request.email())) {
            log.warn("[SERVICE] User {} is already a member of workspace {}", request.email(), workspaceId);
            throw new DuplicateResourceException("User is already a member");
        }

        if (invitationRepository.existsByWorkspaceIdAndEmailAndStatus(workspaceId, request.email(), "PENDING")) {
            log.warn("[SERVICE] Invitation already pending for {} in workspace {}", request.email(), workspaceId);
            throw new DuplicateResourceException("An invitation is already pending for this email");
        }

        String token = UUID.randomUUID().toString();
        WorkspaceInvitation invitation = WorkspaceInvitation.builder()
                .workspaceId(workspaceId)
                .email(request.email())
                .role(request.role())
                .inviterId(currentUser.getId())
                .token(token)
                .status("PENDING")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        invitationRepository.save(invitation);
        log.info("[SERVICE] Invitation saved to DB, token: {}", token);

        User inviter = userRepository.findById(currentUser.getId()).orElseThrow();
        
        String recipientName = userRepository.findByEmail(request.email())
                .map(User::getUsername)
                .orElse("there");

        log.info("[SERVICE] Queuing workspace invitation email asynchronously for: {}", request.email());
        emailNotificationService.sendWorkspaceInvitationEmail(
            inviter,
            request.email(),
            recipientName,
            workspace.getName(),
            token
        );
        log.info("[SERVICE] Workspace invitation email queued for: {}", request.email());
    }

    @Override
    @Transactional
    public void acceptInvitation(String token, UserPrincipal currentUser) {
        WorkspaceInvitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        if (!invitation.getStatus().equals("PENDING")) {
            throw new IllegalStateException("Invitation is no longer pending");
        }

        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            invitation.setStatus("EXPIRED");
            invitationRepository.save(invitation);
            throw new IllegalStateException("Invitation has expired");
        }

        User user = userRepository.findById(currentUser.getId()).orElseThrow();
        if (!user.getEmail().equalsIgnoreCase(invitation.getEmail())) {
            throw new UnauthorizedException("This invitation was sent to a different email address");
        }

        // Add to workspace
        WorkspaceMember member = WorkspaceMember.builder()
                .workspaceId(invitation.getWorkspaceId())
                .userId(user.getId())
                .role(invitation.getRole())
                .build();
        memberRepository.save(member);

        invitation.setStatus("ACCEPTED");
        invitationRepository.save(invitation);

        log.info("User {} accepted invitation to workspace {}", user.getEmail(), invitation.getWorkspaceId());
    }

    @Override
    @Transactional
    public void rejectInvitation(String token, UserPrincipal currentUser) {
        WorkspaceInvitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        User user = userRepository.findById(currentUser.getId()).orElseThrow();
        if (!user.getEmail().equalsIgnoreCase(invitation.getEmail())) {
            throw new UnauthorizedException("You cannot reject this invitation");
        }

        invitation.setStatus("REJECTED");
        invitationRepository.save(invitation);
        log.info("User {} rejected invitation to workspace {}", user.getEmail(), invitation.getWorkspaceId());
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

        long boardCount = boardRepository.countByWorkspaceId(workspace.getId());

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
                .boardCount(boardCount)
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
