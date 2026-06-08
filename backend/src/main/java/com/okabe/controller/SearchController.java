package com.okabe.controller;

import com.okabe.dto.response.ApiResponse;
import com.okabe.dto.response.SearchResultItem;
import com.okabe.security.UserPrincipal;
import com.okabe.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Global search API")
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/global")
    @Operation(summary = "Global fuzzy search across boards, cards, members, and workspaces")
    public ResponseEntity<ApiResponse<List<SearchResultItem>>> globalSearch(
            @RequestParam String q,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(searchService.globalSearch(q, currentUser))); // Tìm kiếm toàn bộ dữ liệu (boards, cards, members, workspaces)
    }
}
