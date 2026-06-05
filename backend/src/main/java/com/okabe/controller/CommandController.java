package com.okabe.controller;

import com.okabe.dto.request.CommandRequest;
import com.okabe.dto.response.ApiResponse;
import com.okabe.dto.response.CommandResponse;
import com.okabe.security.UserPrincipal;
import com.okabe.service.CommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/commands")
@RequiredArgsConstructor
@Tag(name = "Commands", description = "Slash command execution API")
public class CommandController {

    private final CommandService commandService;

    @PostMapping("/execute")
    @Operation(summary = "Execute a slash command")
    public ResponseEntity<ApiResponse<CommandResponse>> execute(
            @Valid @RequestBody CommandRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(commandService.execute(request.command(), currentUser)));
    }
}
