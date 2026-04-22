package com.okabe.service;

import com.okabe.dto.response.BoardAnalyticsResponse;
import com.okabe.security.UserPrincipal;

public interface BoardAnalyticsService {
    BoardAnalyticsResponse getBoardAnalytics(Long boardId, UserPrincipal currentUser);
}
