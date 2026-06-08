package com.okabe.controller;

import com.okabe.dto.response.ApiResponse;
import com.okabe.dto.response.WorkloadResponse;
import com.okabe.security.UserPrincipal;
import com.okabe.service.WorkloadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/workload")
@RequiredArgsConstructor
@Tag(name = "Workload", description = "Workload heatmap APIs")
public class WorkloadController {

    private final WorkloadService workloadService;

    @GetMapping
    @Operation(summary = "Get workload heatmap for workspace members")
    public ResponseEntity<ApiResponse<WorkloadResponse>> getWorkload(
            @PathVariable Long workspaceId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        WorkloadResponse workload = workloadService.getWorkload(workspaceId, from, to, currentUser); // Tính toán workload heatmap
        return ResponseEntity.ok(ApiResponse.success(workload));
    }
}
