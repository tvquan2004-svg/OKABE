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
    ChecklistItemResponse createChecklistItem(Long checklistId, CreateChecklistItemRequest request, UserPrincipal currentUser);
    ChecklistItemResponse updateChecklistItem(Long itemId, UpdateChecklistItemRequest request, UserPrincipal currentUser);
    
    // Label methods
    LabelResponse createLabel(Long boardId, CreateLabelRequest request, UserPrincipal currentUser);
    List<LabelResponse> getBoardLabels(Long boardId, UserPrincipal currentUser);
    void addLabelToCard(Long cardId, Long labelId, UserPrincipal currentUser);
    void removeLabelFromCard(Long cardId, Long labelId, UserPrincipal currentUser);

    // Member assignment
    void assignMember(Long cardId, Long userId, UserPrincipal currentUser);
    void unassignMember(Long cardId, Long userId, UserPrincipal currentUser);
}
