package com.okabe.service;

import com.okabe.dto.response.SuggestionResponse;
import com.okabe.security.UserPrincipal;

import java.util.List;

public interface SmartSuggestionService {
    List<SuggestionResponse> getSuggestions(Long workspaceId, UserPrincipal currentUser);
    void dismissSuggestion(Long suggestionId, UserPrincipal currentUser);
}
