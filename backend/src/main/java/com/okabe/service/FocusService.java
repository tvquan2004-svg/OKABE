package com.okabe.service;

import com.okabe.dto.response.FocusSessionResponse;
import com.okabe.dto.response.FocusStatsResponse;
import com.okabe.security.UserPrincipal;

public interface FocusService {

    FocusSessionResponse startFocus(Long cardId, int durationMinutes, UserPrincipal currentUser);

    FocusSessionResponse stopFocus(Long cardId, UserPrincipal currentUser);

    FocusStatsResponse getStats(String from, String to, UserPrincipal currentUser);
}
