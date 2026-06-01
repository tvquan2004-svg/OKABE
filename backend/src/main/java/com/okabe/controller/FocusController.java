package com.okabe.controller;

import com.okabe.dto.response.FocusSessionResponse;
import com.okabe.dto.response.FocusStatsResponse;
import com.okabe.security.UserPrincipal;
import com.okabe.service.FocusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FocusController {

    private final FocusService focusService;

    @PostMapping("/cards/{cardId}/focus/start")
    public ResponseEntity<FocusSessionResponse> startFocus(
            @PathVariable Long cardId,
            @RequestParam(required = false, defaultValue = "25") Integer durationMinutes,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(focusService.startFocus(cardId, durationMinutes, currentUser));
    }

    @PostMapping("/cards/{cardId}/focus/stop")
    public ResponseEntity<FocusSessionResponse> stopFocus(
            @PathVariable Long cardId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(focusService.stopFocus(cardId, currentUser));
    }

    @GetMapping("/users/me/focus-stats")
    public ResponseEntity<FocusStatsResponse> getStats(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(focusService.getStats(from, to, currentUser));
    }
}
