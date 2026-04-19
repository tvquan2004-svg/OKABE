package com.okabe.controller;

import com.okabe.dto.response.ApiResponse;
import com.okabe.dto.response.AttachmentResponse;
import com.okabe.security.UserPrincipal;
import com.okabe.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Attachment", description = "Attachment management APIs")
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping("/cards/{cardId}/attachments")
    @Operation(summary = "Upload attachment to card")
    public ResponseEntity<ApiResponse<AttachmentResponse>> uploadAttachment(
            @PathVariable Long cardId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal currentUser) throws IOException {
        return ResponseEntity.ok(ApiResponse.success(attachmentService.uploadAttachment(cardId, file, currentUser)));
    }

    @GetMapping("/cards/{cardId}/attachments")
    @Operation(summary = "Get card attachments")
    public ResponseEntity<ApiResponse<List<AttachmentResponse>>> getCardAttachments(
            @PathVariable Long cardId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(attachmentService.getCardAttachments(cardId, currentUser)));
    }

    @DeleteMapping("/attachments/{attachmentId}")
    @Operation(summary = "Delete attachment")
    public ResponseEntity<ApiResponse<Void>> deleteAttachment(
            @PathVariable Long attachmentId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        attachmentService.deleteAttachment(attachmentId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
