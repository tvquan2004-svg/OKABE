package com.okabe.controller;

import com.okabe.dto.request.UpdateNotificationPreferenceRequest;
import com.okabe.dto.response.ApiResponse;
import com.okabe.dto.response.NotificationPreferenceResponse;
import com.okabe.entity.NotificationPreference;
import com.okabe.entity.User;
import com.okabe.repository.UserRepository;
import com.okabe.security.UserPrincipal;
import com.okabe.service.NotificationPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me/notification-preferences")
@RequiredArgsConstructor
@Tag(name = "User Preferences", description = "User notification preferences APIs")
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Get current user's notification preferences")
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> getPreferences(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        NotificationPreference pref = preferenceService.getPreferences(currentUser.getId()); // Lấy cài đặt thông báo
        return ResponseEntity.ok(ApiResponse.success(toResponse(pref)));
    }

    @PutMapping
    @Operation(summary = "Update current user's notification preferences")
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> updatePreferences(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody UpdateNotificationPreferenceRequest request) {
        
        try {
            NotificationPreference saved = preferenceService.saveOrUpdatePreferences( // Lưu cài đặt thông báo
                    currentUser.getId(),
                    request.emailAssigned(),
                    request.emailMentioned(),
                    request.emailDueSoon(),
                    request.emailInvited()
            );
            return ResponseEntity.ok(ApiResponse.success(toResponse(saved), "Preferences updated successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to update preferences: " + e.getMessage(), "INTERNAL_SERVER_ERROR"));
        }
    }

    private NotificationPreferenceResponse toResponse(NotificationPreference pref) {
        return new NotificationPreferenceResponse(
                pref.isEmailAssigned(),
                pref.isEmailMentioned(),
                pref.isEmailDueSoon(),
                pref.isEmailInvited()
        );
    }
}
