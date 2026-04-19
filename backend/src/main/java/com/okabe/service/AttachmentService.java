package com.okabe.service;

import com.okabe.dto.response.AttachmentResponse;
import com.okabe.security.UserPrincipal;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

public interface AttachmentService {
    AttachmentResponse uploadAttachment(Long cardId, MultipartFile file, UserPrincipal currentUser) throws IOException;
    List<AttachmentResponse> getCardAttachments(Long cardId, UserPrincipal currentUser);
    void deleteAttachment(Long attachmentId, UserPrincipal currentUser);
}
