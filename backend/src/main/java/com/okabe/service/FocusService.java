package com.okabe.service;

import com.okabe.dto.response.FocusSessionResponse;
import com.okabe.dto.response.FocusStatsResponse;
import com.okabe.security.UserPrincipal;

public interface FocusService {

    // Bắt đầu phiên tập trung cho card
    FocusSessionResponse startFocus(Long cardId, int durationMinutes, UserPrincipal currentUser);

    // Kết thúc phiên tập trung cho card
    FocusSessionResponse stopFocus(Long cardId, UserPrincipal currentUser);

    // Lấy thống kê tập trung theo khoảng thời gian
    FocusStatsResponse getStats(String from, String to, UserPrincipal currentUser);
}
