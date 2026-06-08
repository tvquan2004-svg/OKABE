package com.okabe.controller;

import com.okabe.dto.response.ApiResponse;
import com.okabe.dto.response.BoardAnalyticsResponse;
import com.okabe.security.UserPrincipal;
import com.okabe.service.BoardAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/boards")
@RequiredArgsConstructor
@Tag(name = "Board Analytics", description = "Endpoints for board reporting and analytics")
public class BoardAnalyticsController {

    private final BoardAnalyticsService boardAnalyticsService;

    @GetMapping("/{id}/analytics")
    @Operation(summary = "Get board analytics data")
    public ResponseEntity<ApiResponse<BoardAnalyticsResponse>> getBoardAnalytics(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(boardAnalyticsService.getBoardAnalytics(id, currentUser))); // Lấy dữ liệu thống kê bảng
    }
}
