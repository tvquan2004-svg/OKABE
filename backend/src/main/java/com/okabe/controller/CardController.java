package com.okabe.controller;

import com.okabe.dto.request.CreateCardRequest;
import com.okabe.dto.request.MoveCardRequest;
import com.okabe.dto.request.UpdateCardRequest;
import com.okabe.dto.response.ApiResponse;
import com.okabe.dto.response.CardResponse;
import com.okabe.security.UserPrincipal;
import com.okabe.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Card", description = "Card (task) management APIs")
public class CardController {

    private final CardService cardService;

    @GetMapping("/api/v1/cards/{id}")
    @Operation(summary = "Get card detail")
    public ResponseEntity<ApiResponse<CardResponse>> getCard(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(cardService.getCard(id, currentUser)));
    }

    @PostMapping("/api/v1/lists/{listId}/cards")
    @Operation(summary = "Create a new card in a list")
    public ResponseEntity<ApiResponse<CardResponse>> createCard(
            @PathVariable Long listId,
            @Valid @RequestBody CreateCardRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(cardService.createCard(listId, request, currentUser),
                        "Card created"));
    }

    @PutMapping("/api/v1/cards/{id}")
    @Operation(summary = "Update a card")
    public ResponseEntity<ApiResponse<CardResponse>> updateCard(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCardRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                cardService.updateCard(id, request, currentUser)));
    }

    @PutMapping("/api/v1/cards/{id}/move")
    @Operation(summary = "Move a card to another list")
    public ResponseEntity<ApiResponse<CardResponse>> moveCard(
            @PathVariable Long id,
            @Valid @RequestBody MoveCardRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                cardService.moveCard(id, request, currentUser), "Card moved"));
    }

    @DeleteMapping("/api/v1/cards/{id}")
    @Operation(summary = "Delete a card")
    public ResponseEntity<ApiResponse<Void>> deleteCard(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        cardService.deleteCard(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(null, "Card deleted"));
    }
}
