package com.okabe.controller;

import com.okabe.dto.request.*;
import com.okabe.dto.response.*;
import com.okabe.security.UserPrincipal;
import com.okabe.service.CardService;
import org.springframework.data.domain.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/api/v1/boards/{boardId}/cards/search")
    @Operation(summary = "Search and filter cards across a board")
    public ResponseEntity<ApiResponse<Page<CardResponse>>> searchCards(
            @PathVariable Long boardId,
            CardSearchRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(cardService.searchCards(boardId, request, currentUser)));
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

    // ─── Checklist Endpoints ───────────────────────────

    @PostMapping("/api/v1/cards/{cardId}/checklists")
    @Operation(summary = "Create a checklist for a card")
    public ResponseEntity<ApiResponse<ChecklistResponse>> createChecklist(
            @PathVariable Long cardId,
            @Valid @RequestBody CreateChecklistRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(cardService.createChecklist(cardId, request, currentUser)));
    }

    @PutMapping("/api/v1/checklists/{checklistId}")
    @Operation(summary = "Update a checklist")
    public ResponseEntity<ApiResponse<ChecklistResponse>> updateChecklist(
            @PathVariable Long checklistId,
            @Valid @RequestBody CreateChecklistRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(cardService.updateChecklist(checklistId, request, currentUser)));
    }

    @DeleteMapping("/api/v1/checklists/{checklistId}")
    @Operation(summary = "Delete a checklist")
    public ResponseEntity<ApiResponse<Void>> deleteChecklist(
            @PathVariable Long checklistId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        cardService.deleteChecklist(checklistId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(null, "Checklist deleted"));
    }

    @PostMapping("/api/v1/checklists/{checklistId}/items")
    @Operation(summary = "Add an item to a checklist")
    public ResponseEntity<ApiResponse<ChecklistItemResponse>> createChecklistItem(
            @PathVariable Long checklistId,
            @Valid @RequestBody CreateChecklistItemRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(cardService.createChecklistItem(checklistId, request, currentUser)));
    }

    @PutMapping("/api/v1/checklists/items/{itemId}")
    @Operation(summary = "Update a checklist item")
    public ResponseEntity<ApiResponse<ChecklistItemResponse>> updateChecklistItem(
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateChecklistItemRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(cardService.updateChecklistItem(itemId, request, currentUser)));
    }

    @DeleteMapping("/api/v1/checklists/items/{itemId}")
    @Operation(summary = "Delete a checklist item")
    public ResponseEntity<ApiResponse<Void>> deleteChecklistItem(
            @PathVariable Long itemId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        cardService.deleteChecklistItem(itemId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(null, "Checklist item deleted"));
    }

    // ─── Label Endpoints ───────────────────────────────

    @PostMapping("/api/v1/boards/{boardId}/labels")
    @Operation(summary = "Create a new label for a board")
    public ResponseEntity<ApiResponse<LabelResponse>> createLabel(
            @PathVariable Long boardId,
            @Valid @RequestBody CreateLabelRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(cardService.createLabel(boardId, request, currentUser)));
    }

    @GetMapping("/api/v1/boards/{boardId}/labels")
    @Operation(summary = "Get all labels in a board")
    public ResponseEntity<ApiResponse<List<LabelResponse>>> getBoardLabels(
            @PathVariable Long boardId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(cardService.getBoardLabels(boardId, currentUser)));
    }

    @PutMapping("/api/v1/labels/{id}")
    @Operation(summary = "Update a label")
    public ResponseEntity<ApiResponse<LabelResponse>> updateLabel(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLabelRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(cardService.updateLabel(id, request, currentUser)));
    }

    @DeleteMapping("/api/v1/labels/{id}")
    @Operation(summary = "Delete a label")
    public ResponseEntity<ApiResponse<Void>> deleteLabel(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        cardService.deleteLabel(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(null, "Label deleted"));
    }

    @PostMapping("/api/v1/cards/{cardId}/labels/{labelId}")
    @Operation(summary = "Add a label to a card")
    public ResponseEntity<ApiResponse<Void>> addLabelToCard(
            @PathVariable Long cardId,
            @PathVariable Long labelId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        cardService.addLabelToCard(cardId, labelId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(null, "Label added to card"));
    }

    @DeleteMapping("/api/v1/cards/{cardId}/labels/{labelId}")
    @Operation(summary = "Remove a label from a card")
    public ResponseEntity<ApiResponse<Void>> removeLabelFromCard(
            @PathVariable Long cardId,
            @PathVariable Long labelId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        cardService.removeLabelFromCard(cardId, labelId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(null, "Label removed from card"));
    }

    // ─── Member Endpoints ──────────────────────────────

    @PostMapping("/api/v1/cards/{cardId}/members")
    @Operation(summary = "Assign a member to a card")
    public ResponseEntity<ApiResponse<Void>> assignMember(
            @PathVariable Long cardId,
            @Valid @RequestBody AssignMemberRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        cardService.assignMember(cardId, request.userId(), currentUser);
        return ResponseEntity.ok(ApiResponse.success(null, "Member assigned"));
    }

    @DeleteMapping("/api/v1/cards/{cardId}/members/{userId}")
    @Operation(summary = "Unassign a member from a card")
    public ResponseEntity<ApiResponse<Void>> unassignMember(
            @PathVariable Long cardId,
            @PathVariable Long userId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        cardService.unassignMember(cardId, userId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(null, "Member unassigned"));
    }

    @PutMapping("/api/v1/cards/{id}/archive")
    @Operation(summary = "Archive a card")
    public ResponseEntity<ApiResponse<CardResponse>> archiveCard(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                cardService.archiveCard(id, currentUser), "Card archived"));
    }

    @PutMapping("/api/v1/cards/{id}/restore")
    @Operation(summary = "Restore a card")
    public ResponseEntity<ApiResponse<CardResponse>> restoreCard(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                cardService.restoreCard(id, currentUser), "Card restored"));
    }

    @GetMapping("/api/v1/boards/{boardId}/cards/archived")
    @Operation(summary = "Get archived cards in a board")
    public ResponseEntity<ApiResponse<Page<CardResponse>>> getArchivedCards(
            @PathVariable Long boardId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                cardService.getArchivedCards(boardId, page, size, currentUser)));
    }
}
