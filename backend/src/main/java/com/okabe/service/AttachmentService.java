package com.okabe.service;

import com.okabe.dto.response.AttachmentResponse;
import com.okabe.security.UserPrincipal;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

public interface AttachmentService {
    // Upload tệp đính kèm vào card
    AttachmentResponse uploadAttachment(Long cardId, MultipartFile file, UserPrincipal currentUser) throws IOException;
    // Lấy danh sách tệp đính kèm của card
    List<AttachmentResponse> getCardAttachments(Long cardId, UserPrincipal currentUser);
    // Xoá tệp đính kèm
    void deleteAttachment(Long attachmentId, UserPrincipal currentUser);
}
