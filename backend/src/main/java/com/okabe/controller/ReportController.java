package com.okabe.controller;

import com.okabe.security.UserPrincipal;
import com.okabe.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Report", description = "Export board/workspace reports as PDF or Excel")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/api/v1/boards/{boardId}/export")
    @Operation(summary = "Export board report as PDF or Excel")
    public ResponseEntity<byte[]> exportBoard(
            @PathVariable Long boardId,
            @RequestParam(defaultValue = "pdf") String format,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        byte[] data = reportService.exportBoardReport(boardId, format, currentUser);
        return buildFileResponse(data, format, "board-report-" + boardId);
    }

    @GetMapping("/api/v1/workspaces/{workspaceId}/export")
    @Operation(summary = "Export workspace report as PDF or Excel")
    public ResponseEntity<byte[]> exportWorkspace(
            @PathVariable Long workspaceId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "pdf") String format,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        byte[] data = reportService.exportWorkspaceReport(workspaceId, from, to, format, currentUser);
        return buildFileResponse(data, format, "workspace-report-" + workspaceId);
    }

    private ResponseEntity<byte[]> buildFileResponse(byte[] data, String format, String filename) {
        String ext;
        MediaType contentType;
        if ("excel".equalsIgnoreCase(format)) {
            ext = ".xlsx";
            contentType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        } else {
            ext = ".pdf";
            contentType = MediaType.APPLICATION_PDF;
        }

        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + ext + "\"")
                .body(data);
    }
}
