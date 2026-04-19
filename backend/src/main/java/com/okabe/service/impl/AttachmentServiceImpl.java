package com.okabe.service.impl;

import com.okabe.dto.response.AttachmentResponse;
import com.okabe.entity.Attachment;
import com.okabe.entity.Card;
import com.okabe.entity.User;
import com.okabe.entity.enums.Role;
import com.okabe.exception.ResourceNotFoundException;
import com.okabe.exception.UnauthorizedException;
import com.okabe.repository.AttachmentRepository;
import com.okabe.repository.CardRepository;
import com.okabe.repository.UserRepository;
import com.okabe.repository.WorkspaceMemberRepository;
import com.okabe.security.UserPrincipal;
import com.okabe.service.ActivityService;
import com.okabe.service.AttachmentService;
import com.okabe.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final StorageService storageService;
    private final ActivityService activityService;

    @Override
    @Transactional
    public AttachmentResponse uploadAttachment(Long cardId, MultipartFile file, UserPrincipal currentUser) throws IOException {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card", cardId));
        
        validateWriteAccess(card, currentUser.getId());

        User uploader = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUser.getId()));

        String url = storageService.upload(file);

        Attachment attachment = Attachment.builder()
                .card(card)
                .uploadedBy(uploader)
                .filename(file.getOriginalFilename())
                .storageKey(url)
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .build();

        attachment = attachmentRepository.save(attachment);
        
        activityService.logActivity(card, uploader, "ADD_ATTACHMENT", "attached " + attachment.getFilename() + " to this card");
        log.info("Attachment uploaded: {} for card {}", attachment.getFilename(), cardId);

        return toResponse(attachment);
    }

    @Override
    public List<AttachmentResponse> getCardAttachments(Long cardId, UserPrincipal currentUser) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card", cardId));
        
        validateMembership(card, currentUser.getId());

        return attachmentRepository.findByCardIdOrderByCreatedAtDesc(cardId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteAttachment(Long attachmentId, UserPrincipal currentUser) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", attachmentId));

        // Only uploader or board ADMIN/OWNER can delete
        boolean isUploader = attachment.getUploadedBy().getId().equals(currentUser.getId());
        boolean hasAdminAccess = hasAdminAccess(attachment.getCard().getTaskList().getBoard().getWorkspace().getId(), currentUser.getId());

        if (!isUploader && !hasAdminAccess) {
            throw new UnauthorizedException("You don't have permission to delete this attachment");
        }

        storageService.delete(attachment.getStorageKey());
        
        User actor = userRepository.findById(currentUser.getId()).orElse(null);
        activityService.logActivity(attachment.getCard(), actor, "DELETE_ATTACHMENT", "deleted attachment " + attachment.getFilename());
        
        attachmentRepository.delete(attachment);
        log.info("Attachment deleted: {}", attachmentId);
    }

    private void validateMembership(Card card, Long userId) {
        Long workspaceId = card.getTaskList().getBoard().getWorkspace().getId();
        if (!memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new UnauthorizedException("You are not a member of this workspace");
        }
    }

    private void validateWriteAccess(Card card, Long userId) {
        Long workspaceId = card.getTaskList().getBoard().getWorkspace().getId();
        boolean hasAccess = memberRepository.existsByWorkspaceIdAndUserIdAndRoleIn(
                workspaceId, userId, List.of(Role.OWNER, Role.ADMIN, Role.MEMBER));
        if (!hasAccess) {
            throw new UnauthorizedException("VIEWERs cannot perform this action");
        }
    }

    private boolean hasAdminAccess(Long workspaceId, Long userId) {
        return memberRepository.existsByWorkspaceIdAndUserIdAndRoleIn(
                workspaceId, userId, List.of(Role.OWNER, Role.ADMIN));
    }

    private AttachmentResponse toResponse(Attachment attachment) {
        return AttachmentResponse.builder()
                .id(attachment.getId())
                .cardId(attachment.getCard().getId())
                .uploadedById(attachment.getUploadedBy().getId())
                .uploadedByUsername(attachment.getUploadedBy().getUsername())
                .filename(attachment.getFilename())
                .url(attachment.getStorageKey())
                .fileSize(attachment.getFileSize())
                .mimeType(attachment.getMimeType())
                .createdAt(attachment.getCreatedAt())
                .build();
    }
}
