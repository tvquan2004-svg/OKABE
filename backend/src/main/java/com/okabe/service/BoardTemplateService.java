package com.okabe.service;

import com.okabe.dto.request.SaveAsTemplateRequest;
import com.okabe.dto.response.BoardTemplateResponse;
import com.okabe.security.UserPrincipal;

import java.util.List;

public interface BoardTemplateService {
    // Lấy tất cả template board (hệ thống và của workspace)
    List<BoardTemplateResponse> getAllTemplates(Long workspaceId, UserPrincipal currentUser);
    // Lấy thông tin template theo id
    BoardTemplateResponse getTemplate(Long templateId, UserPrincipal currentUser);
    // Xoá template
    void deleteTemplate(Long templateId, UserPrincipal currentUser);
    // Lưu board hiện tại thành template
    BoardTemplateResponse saveAsTemplate(Long boardId, SaveAsTemplateRequest request, UserPrincipal currentUser);
}
