package com.okabe.service.impl;

import com.okabe.dto.request.CreateCardRequest;
import com.okabe.dto.request.MoveCardRequest;
import com.okabe.dto.request.UpdateCardRequest;
import com.okabe.dto.response.CardResponse;
import com.okabe.entity.Card;
import com.okabe.entity.TaskList;
import com.okabe.entity.User;
import com.okabe.entity.enums.Priority;
import com.okabe.exception.ResourceNotFoundException;
import com.okabe.exception.UnauthorizedException;
import com.okabe.repository.*;
import com.okabe.security.UserPrincipal;
import com.okabe.service.CardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardServiceImpl implements CardService {

    private final CardRepository cardRepository;
    private final TaskListRepository taskListRepository;
    private final UserRepository userRepository;
    private final WorkspaceMemberRepository memberRepository;

    @Override
    public CardResponse getCard(Long cardId, UserPrincipal currentUser) {
        Card card = findCardOrThrow(cardId);
        validateMembership(card, currentUser.getId());
        return toCardResponse(card);
    }

    @Override
    @Transactional
    public CardResponse createCard(Long listId, CreateCardRequest request, UserPrincipal currentUser) {
        TaskList taskList = findListOrThrow(listId);
        validateMembership(taskList, currentUser.getId());

        User creator = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUser.getId()));

        int nextPosition = cardRepository.countByTaskListIdAndIsArchivedFalse(listId);

        Priority priority = Priority.MEDIUM;
        if (request.priority() != null) {
            try {
                priority = Priority.valueOf(request.priority().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // Keep default MEDIUM
            }
        }

        LocalDateTime dueDate = null;
        if (request.dueDate() != null && !request.dueDate().isBlank()) {
            dueDate = LocalDateTime.parse(request.dueDate());
        }

        Card card = Card.builder()
                .taskList(taskList)
                .title(request.title())
                .description(request.description())
                .position(nextPosition)
                .priority(priority)
                .dueDate(dueDate)
                .createdBy(creator)
                .build();

        card = cardRepository.save(card);
        log.info("Card created: {} in list {}", card.getTitle(), listId);
        return toCardResponse(card);
    }

    @Override
    @Transactional
    public CardResponse updateCard(Long cardId, UpdateCardRequest request, UserPrincipal currentUser) {
        Card card = findCardOrThrow(cardId);
        validateMembership(card, currentUser.getId());

        if (request.title() != null) card.setTitle(request.title());
        if (request.description() != null) card.setDescription(request.description());
        if (request.isArchived() != null) card.setIsArchived(request.isArchived());
        if (request.priority() != null) {
            try {
                card.setPriority(Priority.valueOf(request.priority().toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // Keep current
            }
        }
        if (request.dueDate() != null) {
            card.setDueDate(request.dueDate().isBlank() ? null : LocalDateTime.parse(request.dueDate()));
        }

        card = cardRepository.save(card);
        return toCardResponse(card);
    }

    @Override
    @Transactional
    public CardResponse moveCard(Long cardId, MoveCardRequest request, UserPrincipal currentUser) {
        Card card = findCardOrThrow(cardId);
        validateMembership(card, currentUser.getId());

        TaskList targetList = findListOrThrow(request.targetListId());

        // Update positions in source list (shift cards up)
        List<Card> sourceCards = cardRepository
                .findByTaskListIdAndIsArchivedFalseOrderByPositionAsc(card.getTaskList().getId());
        sourceCards.remove(card);
        for (int i = 0; i < sourceCards.size(); i++) {
            sourceCards.get(i).setPosition(i);
        }
        cardRepository.saveAll(sourceCards);

        // Insert into target list at new position
        List<Card> targetCards = cardRepository
                .findByTaskListIdAndIsArchivedFalseOrderByPositionAsc(targetList.getId());
        int newPos = Math.min(request.newPosition(), targetCards.size());

        card.setTaskList(targetList);
        card.setPosition(newPos);

        // Shift existing cards in target list
        for (Card tc : targetCards) {
            if (tc.getPosition() >= newPos) {
                tc.setPosition(tc.getPosition() + 1);
            }
        }
        cardRepository.saveAll(targetCards);
        card = cardRepository.save(card);

        log.info("Card {} moved to list {} at position {}", cardId, request.targetListId(), newPos);
        return toCardResponse(card);
    }

    @Override
    @Transactional
    public void deleteCard(Long cardId, UserPrincipal currentUser) {
        Card card = findCardOrThrow(cardId);
        validateMembership(card, currentUser.getId());
        cardRepository.delete(card);
        log.info("Card deleted: {}", cardId);
    }

    // ─── Helpers ────────────────────────────────────────

    private Card findCardOrThrow(Long id) {
        return cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Card", id));
    }

    private TaskList findListOrThrow(Long id) {
        return taskListRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("List", id));
    }

    private void validateMembership(Card card, Long userId) {
        Long workspaceId = card.getTaskList().getBoard().getWorkspace().getId();
        if (!memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new UnauthorizedException("You are not a member of this workspace");
        }
    }

    private void validateMembership(TaskList taskList, Long userId) {
        Long workspaceId = taskList.getBoard().getWorkspace().getId();
        if (!memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new UnauthorizedException("You are not a member of this workspace");
        }
    }

    private CardResponse toCardResponse(Card card) {
        return CardResponse.builder()
                .id(card.getId())
                .listId(card.getTaskList().getId())
                .title(card.getTitle())
                .description(card.getDescription())
                .position(card.getPosition())
                .dueDate(card.getDueDate())
                .priority(card.getPriority().name())
                .isArchived(card.getIsArchived())
                .createdById(card.getCreatedBy().getId())
                .createdByName(card.getCreatedBy().getUsername())
                .createdAt(card.getCreatedAt())
                .build();
    }
}
