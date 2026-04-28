package com.okabe.controller;

import com.okabe.dto.response.ApiResponse;
import com.okabe.service.ScheduledNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dev/Admin-only endpoint for manually triggering scheduled tasks.
 * Useful for testing without waiting for the cron schedule.
 * NOTE: Should be protected or removed in production.
 */
@RestController
@RequestMapping("/api/v1/admin/test")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Test", description = "Endpoints for manually triggering scheduled jobs (dev use only)")
public class AdminTestController {

    private final ScheduledNotificationService scheduledNotificationService;

    @PostMapping("/trigger-due-soon")
    @Operation(summary = "Manually trigger the due-date email notification scheduler")
    public ResponseEntity<ApiResponse<String>> triggerDueSoon() {
        log.warn("[AdminTest] Manually triggering due-date notification scheduler...");
        scheduledNotificationService.checkCardsDueSoon();
        return ResponseEntity.ok(ApiResponse.success(null, "Scheduler triggered successfully. Check logs and email inbox."));
    }
}
