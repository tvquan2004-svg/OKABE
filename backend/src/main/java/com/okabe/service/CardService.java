package com.okabe.service;

import com.okabe.dto.request.*;
import com.okabe.dto.response.*;
import com.okabe.security.UserPrincipal;

import org.springframework.data.domain.Page;

import java.util.List;

public interface CardService {

    Page<CardResponse> searchCards(Long boardId, CardSearchRequest request, UserPrincipal currentUser);

    CardResponse getCard(Long cardId, UserPrincipal currentUser);

    CardResponse createCard(Long listId, CreateCardRequest request, UserPrincipal currentUser);

    CardResponse updateCard(Long cardId, UpdateCardRequest request, UserPrincipal currentUser);
    
    CardResponse moveCard(Long cardId, MoveCardRequest request, UserPrincipal currentUser);

    void deleteCard(Long cardId, UserPrincipal currentUser);

    // Checklist methods
    ChecklistResponse createChecklist(Long cardId, CreateChecklistRequest request, UserPrincipal currentUser);
    ChecklistResponse updateChecklist(Long checklistId, CreateChecklistRequest request, UserPrincipal currentUser);
    void deleteChecklist(Long checklistId, UserPrincipal currentUser);
    ChecklistItemResponse createChecklistItem(Long checklistId, CreateChecklistItemRequest request, UserPrincipal currentUser);
    ChecklistItemResponse updateChecklistItem(Long itemId, UpdateChecklistItemRequest request, UserPrincipal currentUser);
    void deleteChecklistItem(Long itemId, UserPrincipal currentUser);
    
    // Label methods
    LabelResponse createLabel(Long boardId, CreateLabelRequest request, UserPrincipal currentUser);
    List<LabelResponse> getBoardLabels(Long boardId, UserPrincipal currentUser);
    LabelResponse updateLabel(Long labelId, UpdateLabelRequest request, UserPrincipal currentUser);
    void deleteLabel(Long labelId, UserPrincipal currentUser);
    void addLabelToCard(Long cardId, Long labelId, UserPrincipal currentUser);
    void removeLabelFromCard(Long cardId, Long labelId, UserPrincipal currentUser);

    // Member assignment
    void assignMember(Long cardId, Long userId, UserPrincipal currentUser);
    void unassignMember(Long cardId, Long userId, UserPrincipal currentUser);

    CardResponse archiveCard(Long cardId, UserPrincipal currentUser);

    CardResponse restoreCard(Long cardId, UserPrincipal currentUser);

    Page<CardResponse> getArchivedCards(Long boardId, int page, int size, UserPrincipal currentUser);

    List<CardSelectionResponse> getWorkspaceCards(Long workspaceId, UserPrincipal currentUser);
}
