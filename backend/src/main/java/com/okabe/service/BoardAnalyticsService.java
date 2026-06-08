package com.okabe.service;

import com.okabe.dto.response.BoardAnalyticsResponse;
import com.okabe.security.UserPrincipal;

public interface BoardAnalyticsService {
    // Lấy dữ liệu phân tích thống kê của board
    BoardAnalyticsResponse getBoardAnalytics(Long boardId, UserPrincipal currentUser);
}
