package com.okabe.service;

import com.okabe.dto.request.SaveAsTemplateRequest;
import com.okabe.dto.response.BoardTemplateResponse;
import com.okabe.security.UserPrincipal;

import java.util.List;

public interface BoardTemplateService {
    List<BoardTemplateResponse> getAllTemplates(Long workspaceId, UserPrincipal currentUser);
    BoardTemplateResponse getTemplate(Long templateId, UserPrincipal currentUser);
    void deleteTemplate(Long templateId, UserPrincipal currentUser);
    BoardTemplateResponse saveAsTemplate(Long boardId, SaveAsTemplateRequest request, UserPrincipal currentUser);
}
