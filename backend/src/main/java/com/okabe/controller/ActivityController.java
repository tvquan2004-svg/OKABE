package com.okabe.controller;

import com.okabe.dto.response.ApiResponse;
import com.okabe.dto.response.ActivityResponse;
import com.okabe.service.ActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Activity", description = "Activity log APIs")
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping("/cards/{cardId}/activities")
    @Operation(summary = "Get activity logs for a card")
    public ResponseEntity<ApiResponse<List<ActivityResponse>>> getCardActivities(
            @PathVariable Long cardId) {
        return ResponseEntity.ok(ApiResponse.success(activityService.getCardActivities(cardId))); // Lấy lịch sử hoạt động của thẻ
    }
}
