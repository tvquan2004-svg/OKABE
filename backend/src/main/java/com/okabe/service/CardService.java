package com.okabe.service;

import com.okabe.dto.request.*;
import com.okabe.dto.response.*;
import com.okabe.security.UserPrincipal;

import java.util.List;

public interface CardService {

    CardResponse getCard(Long cardId, UserPrincipal currentUser);

    CardResponse createCard(Long listId, CreateCardRequest request, UserPrincipal currentUser);

    CardResponse updateCard(Long cardId, UpdateCardRequest request, UserPrincipal currentUser);

    CardResponse moveCard(Long cardId, MoveCardRequest request, UserPrincipal currentUser);

    void deleteCard(Long cardId, UserPrincipal currentUser);

    // Checklist methods
    ChecklistResponse createChecklist(Long cardId, CreateChecklistRequest request, UserPrincipal currentUser);
    ChecklistItemResponse createChecklistItem(Long checklistId, CreateChecklistItemRequest request, UserPrincipal currentUser);
    ChecklistItemResponse updateChecklistItem(Long itemId, UpdateChecklistItemRequest request, UserPrincipal currentUser);
    
    // Label methods
    LabelResponse createLabel(Long boardId, CreateLabelRequest request, UserPrincipal currentUser);
    List<LabelResponse> getBoardLabels(Long boardId, UserPrincipal currentUser);
    void addLabelToCard(Long cardId, Long labelId, UserPrincipal currentUser);
    void removeLabelFromCard(Long cardId, Long labelId, UserPrincipal currentUser);
}
