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
        List<Workspace> workspaces = workspaceRepository.findAllByMemberUserId(currentUser.getId()); // Lấy danh sách workspace
        return workspaces.stream() // Chuyển đổi sang phản hồi
                .map(ws -> toResponse(ws, currentUser.getId()))
                .toList();
    }

    @Override
    public WorkspaceResponse getWorkspace(Long workspaceId, UserPrincipal currentUser) {
        Workspace workspace = findWorkspaceOrThrow(workspaceId); // Tìm workspace
        validateMembership(workspaceId, currentUser.getId()); // Kiểm tra quyền
        return toResponse(workspace, currentUser.getId()); // Trả về phản hồi
    }

    @Override
    @Transactional
    public WorkspaceResponse createWorkspace(CreateWorkspaceRequest request, UserPrincipal currentUser) {
        String slug = generateSlug(request.name()); // Tạo slug từ tên

        if (workspaceRepository.existsBySlug(slug)) { // Nếu slug đã tồn tại
            slug = slug + "-" + System.currentTimeMillis(); // Thêm timestamp
        }

        User owner = userRepository.findById(currentUser.getId()) // Tìm người dùng
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUser.getId()));

        Workspace workspace = Workspace.builder() // Xây dựng workspace
                .name(request.name())
                .slug(slug)
                .description(request.description())
                .owner(owner)
                .build();

        workspace = workspaceRepository.save(workspace); // Lưu workspace

        // Add creator as OWNER member
        WorkspaceMember ownerMember = WorkspaceMember.builder() // Tạo thành viên OWNER
                .workspaceId(workspace.getId())
                .userId(currentUser.getId())
                .role(Role.OWNER)
                .build();
        memberRepository.save(ownerMember); // Lưu thành viên

        log.info("Workspace created: {} by user {}", workspace.getName(), currentUser.getEmail()); // Ghi log
        return toResponse(workspace, currentUser.getId()); // Trả về phản hồi
    }

    @Override
    @Transactional
    public WorkspaceResponse updateWorkspace(Long workspaceId, UpdateWorkspaceRequest request,
            UserPrincipal currentUser) {
        Workspace workspace = findWorkspaceOrThrow(workspaceId); // Tìm workspace
        validateAdminAccess(workspaceId, currentUser.getId()); // Kiểm tra quyền admin

        if (request.name() != null && !request.name().isBlank()) { // Nếu có tên mới
            workspace.setName(request.name()); // Cập nhật tên
        }
        if (request.description() != null) { // Nếu có mô tả mới
            workspace.setDescription(request.description()); // Cập nhật mô tả
        }

        workspace = workspaceRepository.save(workspace); // Lưu thay đổi
        log.info("Workspace updated: {} by user {}", workspace.getName(), currentUser.getEmail()); // Ghi log
        return toResponse(workspace, currentUser.getId()); // Trả về phản hồi
    }

    @Override
    @Transactional
    public void deleteWorkspace(Long workspaceId, UserPrincipal currentUser) {
        Workspace workspace = findWorkspaceOrThrow(workspaceId); // Tìm workspace

        if (!workspace.getOwner().getId().equals(currentUser.getId())) { // Nếu không phải chủ sở hữu
            throw new UnauthorizedException("Only the workspace owner can delete it"); // Ném lỗi
        }

        workspaceRepository.delete(workspace); // Xóa workspace
        log.info("Workspace deleted: {} by user {}", workspace.getName(), currentUser.getEmail()); // Ghi log
    }

    // ─── Member Management ──────────────────────────────

    @Override
    public List<com.okabe.dto.response.WorkspaceMemberResponse> getWorkspaceMembers(Long workspaceId,
            UserPrincipal currentUser) {
        validateMembership(workspaceId, currentUser.getId()); // Kiểm tra quyền
        List<WorkspaceMember> members = memberRepository.findByWorkspaceId(workspaceId); // Lấy danh sách thành viên
        return members.stream() // Chuyển đổi sang phản hồi
                .map(this::toMemberResponse)
                .toList();
    }

    @Override
    @Transactional
    public com.okabe.dto.response.WorkspaceMemberResponse addMemberToWorkspace(Long workspaceId,
            com.okabe.dto.request.AddWorkspaceMemberRequest request, UserPrincipal currentUser) {
        return inviteAndAddDirectly(workspaceId, request, currentUser); // Thêm trực tiếp
    }

    private com.okabe.dto.response.WorkspaceMemberResponse inviteAndAddDirectly(Long workspaceId,
            com.okabe.dto.request.AddWorkspaceMemberRequest request, UserPrincipal currentUser) {
        validateAdminAccess(workspaceId, currentUser.getId()); // Kiểm tra quyền admin
        Workspace workspace = findWorkspaceOrThrow(workspaceId); // Tìm workspace

        User userToAdd = userRepository.findByEmail(request.email()) // Tìm người dùng theo email
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.email()));

        if (memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userToAdd.getId())) { // Nếu đã là thành viên
            throw new DuplicateResourceException("User is already a member of this workspace"); // Ném lỗi
        }

        WorkspaceMember member = WorkspaceMember.builder() // Xây dựng thành viên mới
                .workspaceId(workspaceId)
                .userId(userToAdd.getId())
                .role(request.role() != null ? request.role() : Role.MEMBER)
                .build();

        member = memberRepository.save(member); // Lưu thành viên
        member.setUser(userToAdd); // Gán user

        User actor = userRepository.findById(currentUser.getId()).orElseThrow(); // Lấy người thực hiện
        notificationService.createNotification( // Tạo thông báo
            userToAdd,
            actor,
            "WORKSPACE_JOINED",
            "WORKSPACE",
            workspaceId,
            workspaceId,
            String.format("%s đã thêm bạn vào không gian làm việc: %s", actor.getUsername(), workspace.getName())
        );

        emailNotificationService.sendWorkspaceAddedEmail( // Gửi email thông báo
            actor,
            userToAdd,
            workspace.getName(),
            workspaceId
        );

        log.info("User {} added directly to workspace {} by {}", request.email(), workspaceId, currentUser.getEmail()); // Ghi log
        return toMemberResponse(member); // Trả về phản hồi
    }

    @Override
    @Transactional
    public void inviteMember(Long workspaceId, com.okabe.dto.request.AddWorkspaceMemberRequest request, UserPrincipal currentUser) {
        log.info("[SERVICE] inviteMember triggered for workspace: {}, email: {}", workspaceId, request.email()); // Ghi log
        validateAdminAccess(workspaceId, currentUser.getId()); // Kiểm tra quyền admin
        Workspace workspace = findWorkspaceOrThrow(workspaceId); // Tìm workspace

        if (memberRepository.existsByWorkspaceIdAndEmail(workspaceId, request.email())) { // Nếu đã là thành viên
            log.warn("[SERVICE] User {} is already a member of workspace {}", request.email(), workspaceId);
            throw new DuplicateResourceException("User is already a member"); // Ném lỗi
        }

        if (invitationRepository.existsByWorkspaceIdAndEmailAndStatus(workspaceId, request.email(), "PENDING")) { // Nếu đã có lời mời
            log.warn("[SERVICE] Invitation already pending for {} in workspace {}", request.email(), workspaceId);
            throw new DuplicateResourceException("An invitation is already pending for this email"); // Ném lỗi
        }

        String token = UUID.randomUUID().toString(); // Tạo token
        WorkspaceInvitation invitation = WorkspaceInvitation.builder() // Xây dựng lời mời
                .workspaceId(workspaceId)
                .email(request.email())
                .role(request.role())
                .inviterId(currentUser.getId())
                .token(token)
                .status("PENDING")
                .expiresAt(LocalDateTime.now().plusDays(7)) // Hết hạn sau 7 ngày
                .build();

        invitationRepository.save(invitation); // Lưu lời mời
        log.info("[SERVICE] Invitation saved to DB, token: {}", token); // Ghi log

        User inviter = userRepository.findById(currentUser.getId()).orElseThrow(); // Lấy người mời
        
        String recipientName = "there"; // Tên mặc định
        java.util.Optional<User> existingUser = userRepository.findByEmail(request.email()); // Tìm người nhận
        if (existingUser.isPresent()) { // Nếu người dùng tồn tại
            recipientName = existingUser.get().getUsername(); // Lấy tên
            notificationService.createNotification( // Tạo thông báo
                existingUser.get(),
                inviter,
                "WORKSPACE_INVITATION",
                "WORKSPACE",
                workspaceId,
                invitation.getId(),
                String.format("Bạn có lời mời tham gia không gian làm việc: %s", workspace.getName())
            );
        }

        log.info("[SERVICE] Queuing workspace invitation email asynchronously for: {}", request.email()); // Ghi log
        emailNotificationService.sendWorkspaceInvitationEmail( // Gửi email mời
            inviter,
            request.email(),
            recipientName,
            workspace.getName(),
            token
        );
        log.info("[SERVICE] Workspace invitation email queued for: {}", request.email()); // Ghi log
    }

    @Override
    @Transactional
    public void acceptInvitation(String token, UserPrincipal currentUser) {
        WorkspaceInvitation invitation = invitationRepository.findByToken(token) // Tìm lời mời theo token
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        if (!invitation.getStatus().equals("PENDING")) { // Nếu không còn ở trạng thái PENDING
            throw new IllegalStateException("Invitation is no longer pending"); // Ném lỗi
        }

        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) { // Nếu đã hết hạn
            invitation.setStatus("EXPIRED"); // Đánh dấu hết hạn
            invitationRepository.save(invitation); // Lưu
            throw new IllegalStateException("Invitation has expired"); // Ném lỗi
        }

        User user = userRepository.findById(currentUser.getId()).orElseThrow(); // Lấy người dùng
        if (!user.getEmail().equalsIgnoreCase(invitation.getEmail())) { // Nếu email không khớp
            throw new UnauthorizedException("This invitation was sent to a different email address"); // Ném lỗi
        }

        // Add to workspace
        WorkspaceMember member = WorkspaceMember.builder() // Tạo thành viên
                .workspaceId(invitation.getWorkspaceId())
                .userId(user.getId())
                .role(invitation.getRole())
                .build();
        memberRepository.save(member); // Lưu thành viên

        invitation.setStatus("ACCEPTED"); // Đánh dấu đã chấp nhận
        invitationRepository.save(invitation); // Lưu

        log.info("User {} accepted invitation to workspace {}", user.getEmail(), invitation.getWorkspaceId()); // Ghi log
    }

    @Override
    @Transactional
    public void rejectInvitation(String token, UserPrincipal currentUser) {
        WorkspaceInvitation invitation = invitationRepository.findByToken(token) // Tìm lời mời
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        User user = userRepository.findById(currentUser.getId()).orElseThrow(); // Lấy người dùng
        if (!user.getEmail().equalsIgnoreCase(invitation.getEmail())) { // Nếu email không khớp
            throw new UnauthorizedException("You cannot reject this invitation"); // Ném lỗi
        }

        invitation.setStatus("REJECTED"); // Đánh dấu từ chối
        invitationRepository.save(invitation); // Lưu
        log.info("User {} rejected invitation to workspace {}", user.getEmail(), invitation.getWorkspaceId()); // Ghi log
    }

    @Override
    @Transactional
    public void acceptInvitationById(Long invitationId, UserPrincipal currentUser) {
        WorkspaceInvitation invitation = invitationRepository.findById(invitationId) // Tìm lời mời theo ID
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        if (!invitation.getStatus().equals("PENDING")) { // Nếu không còn PENDING
            throw new IllegalStateException("Invitation is no longer pending"); // Ném lỗi
        }

        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) { // Nếu hết hạn
            invitation.setStatus("EXPIRED"); // Đánh dấu hết hạn
            invitationRepository.save(invitation); // Lưu
            throw new IllegalStateException("Invitation has expired"); // Ném lỗi
        }

        User user = userRepository.findById(currentUser.getId()).orElseThrow(); // Lấy người dùng
        if (!user.getEmail().equalsIgnoreCase(invitation.getEmail())) { // Nếu email không khớp
            throw new UnauthorizedException("This invitation was sent to a different email address"); // Ném lỗi
        }

        WorkspaceMember member = WorkspaceMember.builder() // Tạo thành viên
                .workspaceId(invitation.getWorkspaceId())
                .userId(user.getId())
                .role(invitation.getRole())
                .build();
        memberRepository.save(member); // Lưu thành viên

        invitation.setStatus("ACCEPTED"); // Đánh dấu chấp nhận
        invitationRepository.save(invitation); // Lưu
        log.info("User {} accepted invitation to workspace {}", user.getEmail(), invitation.getWorkspaceId()); // Ghi log
    }

    @Override
    @Transactional
    public void rejectInvitationById(Long invitationId, UserPrincipal currentUser) {
        WorkspaceInvitation invitation = invitationRepository.findById(invitationId) // Tìm lời mời
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        User user = userRepository.findById(currentUser.getId()).orElseThrow(); // Lấy người dùng
        if (!user.getEmail().equalsIgnoreCase(invitation.getEmail())) { // Nếu email không khớp
            throw new UnauthorizedException("You cannot reject this invitation"); // Ném lỗi
        }

        invitation.setStatus("REJECTED"); // Đánh dấu từ chối
        invitationRepository.save(invitation); // Lưu
        log.info("User {} rejected invitation to workspace {}", user.getEmail(), invitation.getWorkspaceId()); // Ghi log
    }

    @Override
    @Transactional
    public com.okabe.dto.response.WorkspaceMemberResponse updateMemberRole(Long workspaceId, Long memberId,
            com.okabe.dto.request.UpdateMemberRoleRequest request, UserPrincipal currentUser) {
        validateAdminAccess(workspaceId, currentUser.getId()); // Kiểm tra quyền admin

        WorkspaceMember memberToUpdate = memberRepository.findByWorkspaceIdAndUserId(workspaceId, memberId) // Tìm thành viên
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in workspace"));

        if (memberToUpdate.getRole() == Role.OWNER) { // Nếu là OWNER
            WorkspaceMember currentMember = memberRepository // Tìm thành viên hiện tại
                    .findByWorkspaceIdAndUserId(workspaceId, currentUser.getId())
                    .orElseThrow(() -> new UnauthorizedException("Not a member"));
            if (currentMember.getRole() != Role.OWNER) { // Nếu không phải OWNER
                throw new UnauthorizedException("Only OWNER can change another OWNER's role"); // Ném lỗi
            }
        }

        memberToUpdate.setRole(request.role()); // Cập nhật vai trò
        memberToUpdate = memberRepository.save(memberToUpdate); // Lưu thay đổi

        log.info("User {} role updated to {} in workspace {}", memberId, request.role(), workspaceId); // Ghi log
        return toMemberResponse(memberToUpdate); // Trả về phản hồi
    }

    @Override
    @Transactional
    public void removeMemberFromWorkspace(Long workspaceId, Long memberId, UserPrincipal currentUser) {
        validateAdminAccess(workspaceId, currentUser.getId()); // Kiểm tra quyền admin

        WorkspaceMember memberToRemove = memberRepository.findByWorkspaceIdAndUserId(workspaceId, memberId) // Tìm thành viên
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in workspace"));

        if (memberToRemove.getRole() == Role.OWNER) { // Nếu là OWNER
            WorkspaceMember currentMember = memberRepository // Tìm thành viên hiện tại
                    .findByWorkspaceIdAndUserId(workspaceId, currentUser.getId())
                    .orElseThrow(() -> new UnauthorizedException("Not a member"));
            if (currentMember.getRole() != Role.OWNER) { // Nếu không phải OWNER
                throw new UnauthorizedException("Only OWNER can remove another OWNER"); // Ném lỗi
            }
        }

        memberRepository.delete(memberToRemove); // Xóa thành viên
        log.info("User {} removed from workspace {}", memberId, workspaceId); // Ghi log
    }

    private com.okabe.dto.response.WorkspaceMemberResponse toMemberResponse(WorkspaceMember member) {
        User user = member.getUser(); // Lấy user
        if (user == null) { // Nếu user null
            user = userRepository.findById(member.getUserId()).orElse(new User()); // Tìm theo ID
        }
        return com.okabe.dto.response.WorkspaceMemberResponse.builder() // Xây dựng phản hồi
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
        return workspaceRepository.findById(id) // Tìm workspace theo ID
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", id)); // Ném lỗi nếu không tìm thấy
    }

    private void validateMembership(Long workspaceId, Long userId) {
        Workspace workspace = findWorkspaceOrThrow(workspaceId); // Tìm workspace
        // Owner always has access
        if (workspace.getOwner().getId().equals(userId)) { // Nếu là chủ sở hữu
            return; // Cho phép
        }

        if (!memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) { // Nếu không phải thành viên
            throw new UnauthorizedException("You are not a member of this workspace"); // Ném lỗi
        }
    }

    private void validateAdminAccess(Long workspaceId, Long userId) {
        Workspace workspace = findWorkspaceOrThrow(workspaceId); // Tìm workspace
        // Owner always has access
        if (workspace.getOwner().getId().equals(userId)) { // Nếu là chủ sở hữu
            return; // Cho phép
        }

        boolean hasAccess = memberRepository.existsByWorkspaceIdAndUserIdAndRoleIn( // Kiểm tra quyền admin
                workspaceId, userId, List.of(Role.OWNER, Role.ADMIN));
        if (!hasAccess) { // Nếu không có quyền
            throw new UnauthorizedException("Only OWNER or ADMIN can perform this action"); // Ném lỗi
        }
    }

    private WorkspaceResponse toResponse(Workspace workspace, Long currentUserId) {
        List<WorkspaceMember> members = memberRepository.findByWorkspaceId(workspace.getId()); // Lấy danh sách thành viên
        User owner = workspace.getOwner(); // Lấy chủ sở hữu
        
        // Find user's role in the list we already fetched
        String currentRole = members.stream() // Tìm vai trò của người dùng hiện tại
                .filter(m -> m.getUserId().equals(currentUserId))
                .map(m -> m.getRole().name())
                .findFirst()
                .orElse(null);

        // Fallback for owner if not found in members list
        if (currentRole == null && owner.getId().equals(currentUserId)) { // Nếu là chủ sở hữu
            currentRole = Role.OWNER.name(); // Gán vai trò OWNER
        }

        long boardCount = boardRepository.countByWorkspaceId(workspace.getId()); // Đếm số bảng

        return WorkspaceResponse.builder() // Xây dựng phản hồi
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
