package com.okabe.controller;

import com.okabe.dto.response.ApiResponse;
import com.okabe.dto.response.BoardPublicDto;
import com.okabe.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/boards")
@RequiredArgsConstructor
public class PublicBoardController {

    private final BoardService boardService;

    @GetMapping("/{token}")
    public ResponseEntity<ApiResponse<BoardPublicDto>> getPublicBoard(@PathVariable String token) {
        return ResponseEntity.ok(ApiResponse.success(boardService.getPublicBoard(token)));
    }
}
