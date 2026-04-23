package com.okabe.controller;

import com.okabe.dto.response.BoardResponse;
import com.okabe.security.UserPrincipal;
import com.okabe.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/boards")
@RequiredArgsConstructor
public class BoardVisibilityController {

    private final BoardService boardService;

    @PutMapping("/{id}/visibility")
    public ResponseEntity<BoardResponse> updateVisibility(
            @PathVariable Long id,
            @RequestParam boolean isPublic,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(boardService.updateVisibility(id, isPublic, currentUser));
    }
}
