package com.okabe.service;

import com.okabe.dto.response.SuggestionResponse;
import com.okabe.security.UserPrincipal;

import java.util.List;

public interface SmartSuggestionService {
    // Lấy danh sách gợi ý thông minh cho workspace (thẻ quá hạn, sắp đến hạn, etc.)
    List<SuggestionResponse> getSuggestions(Long workspaceId, UserPrincipal currentUser);
    // Bỏ qua gợi ý
    void dismissSuggestion(Long suggestionId, UserPrincipal currentUser);
}
