package com.okabe.service;

import com.okabe.dto.request.CreateCardRequest;
import com.okabe.dto.request.MoveCardRequest;
import com.okabe.dto.request.UpdateCardRequest;
import com.okabe.dto.response.CardResponse;
import com.okabe.security.UserPrincipal;

public interface CardService {

    CardResponse getCard(Long cardId, UserPrincipal currentUser);

    CardResponse createCard(Long listId, CreateCardRequest request, UserPrincipal currentUser);

    CardResponse updateCard(Long cardId, UpdateCardRequest request, UserPrincipal currentUser);

    CardResponse moveCard(Long cardId, MoveCardRequest request, UserPrincipal currentUser);

    void deleteCard(Long cardId, UserPrincipal currentUser);
}
