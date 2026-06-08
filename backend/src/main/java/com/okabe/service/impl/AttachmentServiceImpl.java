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
        Card card = cardRepository.findById(cardId) // Tìm thẻ theo ID
                .orElseThrow(() -> new ResourceNotFoundException("Card", cardId)); // Ném lỗi nếu không tìm thấy thẻ
        
        validateWriteAccess(card, currentUser.getId()); // Kiểm tra quyền ghi của người dùng

        User uploader = userRepository.findById(currentUser.getId()) // Tìm người tải lên theo ID
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUser.getId())); // Ném lỗi nếu không tìm thấy người dùng

        String url = storageService.upload(file); // Tải file lên storage và lấy URL

        Attachment attachment = Attachment.builder()
                .card(card) // Gán thẻ cho attachment
                .uploadedBy(uploader) // Gán người tải lên
                .filename(file.getOriginalFilename()) // Gán tên file gốc
                .storageKey(url) // Gán URL storage
                .fileSize(file.getSize()) // Gán kích thước file
                .mimeType(file.getContentType()) // Gán loại MIME
                .build(); // Xây dựng đối tượng Attachment

        attachment = attachmentRepository.save(attachment); // Lưu attachment vào CSDL
        
        activityService.logActivity(card, uploader, "ADD_ATTACHMENT", "attached " + attachment.getFilename() + " to this card"); // Ghi log hoạt động
        log.info("Attachment uploaded: {} for card {}", attachment.getFilename(), cardId); // Ghi log thông tin

        return toResponse(attachment); // Trả về phản hồi AttachmentResponse
    }

    @Override
    public List<AttachmentResponse> getCardAttachments(Long cardId, UserPrincipal currentUser) {
        Card card = cardRepository.findById(cardId) // Tìm thẻ theo ID
                .orElseThrow(() -> new ResourceNotFoundException("Card", cardId)); // Ném lỗi nếu không tìm thấy thẻ
        
        validateMembership(card, currentUser.getId()); // Kiểm tra quyền thành viên

        return attachmentRepository.findByCardIdOrderByCreatedAtDesc(cardId).stream() // Lấy danh sách attachment theo thẻ, sắp xếp theo thời gian
                .map(this::toResponse) // Chuyển đổi sang AttachmentResponse
                .collect(Collectors.toList()); // Thu thập thành danh sách
    }

    @Override
    @Transactional
    public void deleteAttachment(Long attachmentId, UserPrincipal currentUser) {
        Attachment attachment = attachmentRepository.findById(attachmentId) // Tìm attachment theo ID
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", attachmentId)); // Ném lỗi nếu không tìm thấy

        // Only uploader or board ADMIN/OWNER can delete
        boolean isUploader = attachment.getUploadedBy().getId().equals(currentUser.getId()); // Kiểm tra có phải người tải lên
        boolean hasAdminAccess = hasAdminAccess(attachment.getCard().getTaskList().getBoard().getWorkspace().getId(), currentUser.getId()); // Kiểm tra quyền admin

        if (!isUploader && !hasAdminAccess) { // Nếu không có quyền xóa
            throw new UnauthorizedException("You don't have permission to delete this attachment"); // Ném lỗi không có quyền
        }

        storageService.delete(attachment.getStorageKey()); // Xóa file khỏi storage
        
        User actor = userRepository.findById(currentUser.getId()).orElse(null); // Tìm người thực hiện
        activityService.logActivity(attachment.getCard(), actor, "DELETE_ATTACHMENT", "deleted attachment " + attachment.getFilename()); // Ghi log hoạt động
        
        attachmentRepository.delete(attachment); // Xóa attachment khỏi CSDL
        log.info("Attachment deleted: {}", attachmentId); // Ghi log thông tin
    }

    private void validateMembership(Card card, Long userId) {
        Long workspaceId = card.getTaskList().getBoard().getWorkspace().getId(); // Lấy workspace ID từ card
        if (!memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) { // Nếu không phải thành viên
            throw new UnauthorizedException("You are not a member of this workspace"); // Ném lỗi không có quyền
        }
    }

    private void validateWriteAccess(Card card, Long userId) {
        Long workspaceId = card.getTaskList().getBoard().getWorkspace().getId(); // Lấy workspace ID từ card
        boolean hasAccess = memberRepository.existsByWorkspaceIdAndUserIdAndRoleIn(
                workspaceId, userId, List.of(Role.OWNER, Role.ADMIN, Role.MEMBER)); // Kiểm tra quyền ghi
        if (!hasAccess) { // Nếu không có quyền
            throw new UnauthorizedException("You do not have permission to perform this action"); // Ném lỗi không có quyền
        }
    }

    private boolean hasAdminAccess(Long workspaceId, Long userId) {
        return memberRepository.existsByWorkspaceIdAndUserIdAndRoleIn(
                workspaceId, userId, List.of(Role.OWNER, Role.ADMIN)); // Kiểm tra quyền admin
    }

    private AttachmentResponse toResponse(Attachment attachment) {
        return AttachmentResponse.builder()
                .id(attachment.getId()) // Gán ID attachment
                .cardId(attachment.getCard().getId()) // Gán ID thẻ
                .uploadedById(attachment.getUploadedBy().getId()) // Gán ID người tải lên
                .uploadedByUsername(attachment.getUploadedBy().getUsername()) // Gán tên người tải lên
                .filename(attachment.getFilename()) // Gán tên file
                .url(attachment.getStorageKey()) // Gán URL file
                .fileSize(attachment.getFileSize()) // Gán kích thước file
                .mimeType(attachment.getMimeType()) // Gán loại MIME
                .createdAt(attachment.getCreatedAt()) // Gán thời gian tạo
                .build(); // Xây dựng AttachmentResponse
    }
}
