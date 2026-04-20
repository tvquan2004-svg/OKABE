package com.okabe.service.impl;

import com.okabe.dto.request.*;
import com.okabe.dto.response.*;
import com.okabe.entity.*;
import com.okabe.entity.enums.Priority;
import com.okabe.exception.ResourceNotFoundException;
import com.okabe.exception.UnauthorizedException;
import com.okabe.repository.*;
import com.okabe.security.UserPrincipal;
import com.okabe.service.ActivityService;
import com.okabe.service.CardService;
import com.okabe.service.NotificationService;
import com.okabe.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.okabe.repository.specification.CardSpecification;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardServiceImpl implements CardService {

    private final CardRepository cardRepository;
    private final TaskListRepository taskListRepository;
    private final UserRepository userRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final ChecklistRepository checklistRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final LabelRepository labelRepository;
    private final BoardRepository boardRepository;
    private final ActivityService activityService;
    private final NotificationService notificationService;
    private final WebSocketService webSocketService;

    @Override
    public Page<CardResponse> searchCards(Long boardId, CardSearchRequest request, UserPrincipal currentUser) {
        // Validate board exists and user has access
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board", boardId));
        validateMembership(board.getWorkspace().getId(), currentUser.getId());

        Sort sort = Sort.by(Sort.Direction.ASC, "dueDate").and(Sort.by(Sort.Direction.ASC, "position"));
        PageRequest pageRequest = PageRequest.of(request.getPage(), request.getSize(), sort);

        Specification<Card> spec = CardSpecification.filterByRequest(boardId, request);
        
        return cardRepository.findAll(spec, pageRequest)
                .map(this::toCardResponse);
    }

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
        validateWriteAccess(taskList, currentUser.getId());

        User creator = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUser.getId()));

        int nextPosition = cardRepository.countByTaskListIdAndIsArchivedFalse(listId);

        Priority priority = Priority.MEDIUM;
        if (request.priority() != null) {
            try {
                priority = Priority.valueOf(request.priority().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
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
        
        CardResponse response = toCardResponse(card);
        webSocketService.broadcastToBoard(taskList.getBoard().getId(), "CARD_CREATED", response, currentUser.getId());
        
        return response;
    }

    @Override
    @Transactional
    public CardResponse updateCard(Long cardId, UpdateCardRequest request, UserPrincipal currentUser) {
        Card card = findCardOrThrow(cardId);
        validateWriteAccess(card, currentUser.getId());

        User user = findUserOrThrow(currentUser.getId());

        if (request.title() != null && !request.title().equals(card.getTitle())) {
            activityService.logActivity(card, user, "UPDATE_CARD", "changed title to \"" + request.title() + "\"");
            card.setTitle(request.title());
        }
        if (request.description() != null && !request.description().equals(card.getDescription())) {
            activityService.logActivity(card, user, "UPDATE_CARD", "updated description");
            card.setDescription(request.description());
        }
        if (request.isArchived() != null && !request.isArchived().equals(card.getIsArchived())) {
            activityService.logActivity(card, user, request.isArchived() ? "ARCHIVE_CARD" : "RESTORE_CARD", null);
            card.setIsArchived(request.isArchived());
        }
        if (request.priority() != null) {
            try {
                Priority newPriority = Priority.valueOf(request.priority().toUpperCase());
                if (newPriority != card.getPriority()) {
                    activityService.logActivity(card, user, "UPDATE_CARD", "changed priority to " + newPriority);
                    card.setPriority(newPriority);
                }
            } catch (IllegalArgumentException ignored) {}
        }
        if (request.dueDate() != null) {
            LocalDateTime newDueDate = request.dueDate().isBlank() ? null : LocalDateTime.parse(request.dueDate());
            if (newDueDate != card.getDueDate()) {
                activityService.logActivity(card, user, "UPDATE_CARD", "updated due date");
                card.setDueDate(newDueDate);
            }
        }

        card = cardRepository.save(card);
        CardResponse response = toCardResponse(card);
        webSocketService.broadcastToBoard(card.getTaskList().getBoard().getId(), "CARD_UPDATED", response, currentUser.getId());
        
        return response;
    }

    @Override
    @Transactional
    public CardResponse moveCard(Long cardId, MoveCardRequest request, UserPrincipal currentUser) {
        Card card = findCardOrThrow(cardId);
        validateWriteAccess(card, currentUser.getId());

        TaskList targetList = findListOrThrow(request.targetListId());
        Board sourceBoard = card.getTaskList().getBoard();
        Board targetBoard = targetList.getBoard();

        // Validate write access to target workspace
        validateWriteAccess(targetBoard.getWorkspace().getId(), currentUser.getId());

        User actor = findUserOrThrow(currentUser.getId());

        // 1. Remove from source list and reorder
        List<Card> sourceCards = cardRepository
                .findByTaskListIdAndIsArchivedFalseOrderByPositionAsc(card.getTaskList().getId());
        sourceCards.remove(card);
        for (int i = 0; i < sourceCards.size(); i++) {
            sourceCards.get(i).setPosition(i);
        }
        cardRepository.saveAll(sourceCards);

        // 2. Handle cross-board specific logic
        if (!sourceBoard.getId().equals(targetBoard.getId())) {
            // Clear labels as they are board-specific
            card.getLabels().clear();
            activityService.logActivity(card, actor, "MOVE_CARD", 
                "moved card from board \"" + sourceBoard.getName() + "\" to \"" + targetBoard.getName() + "\"");
        } else {
            activityService.logActivity(card, actor, "MOVE_CARD", 
                "moved card to list \"" + targetList.getName() + "\"");
        }

        // 3. Add to target list and reorder
        List<Card> targetCards = cardRepository
                .findByTaskListIdAndIsArchivedFalseOrderByPositionAsc(targetList.getId());
        
        // Use 0 as default position if not provided, or end of list
        int newPos = (request.position() != null) ? request.position() : targetCards.size();
        newPos = Math.max(0, Math.min(newPos, targetCards.size()));

        card.setTaskList(targetList);
        card.setPosition(newPos);

        for (Card tc : targetCards) {
            if (tc.getPosition() >= newPos) {
                tc.setPosition(tc.getPosition() + 1);
            }
        }
        
        cardRepository.saveAll(targetCards);
        card = cardRepository.save(card);

        log.info("Card {} moved from board {} to board {} list {} at position {}", 
            cardId, sourceBoard.getId(), targetBoard.getId(), targetList.getId(), newPos);
        
        CardResponse response = toCardResponse(card);
        webSocketService.broadcastToBoard(sourceBoard.getId(), "CARD_MOVED", response, currentUser.getId());
        if (!sourceBoard.getId().equals(targetBoard.getId())) {
            webSocketService.broadcastToBoard(targetBoard.getId(), "CARD_MOVED", response, currentUser.getId());
        }
        
        return response;
    }

    @Override
    @Transactional
    public void deleteCard(Long cardId, UserPrincipal currentUser) {
        Card card = findCardOrThrow(cardId);
        Long boardId = card.getTaskList().getBoard().getId();
        cardRepository.delete(card);
        log.info("Card deleted: {}", cardId);
        webSocketService.broadcastToBoard(boardId, "CARD_DELETED", cardId, currentUser.getId());
    }

    // ─── Checklist Management ──────────────────────────

    @Override
    @Transactional
    public ChecklistResponse createChecklist(Long cardId, CreateChecklistRequest request, UserPrincipal currentUser) {
        Card card = findCardOrThrow(cardId);
        validateWriteAccess(card, currentUser.getId());

        int nextPosition = card.getChecklists().size();
        Checklist checklist = Checklist.builder()
                .card(card)
                .name(request.name())
                .position(nextPosition)
                .build();
        
        checklist = checklistRepository.save(checklist);

        User user = findUserOrThrow(currentUser.getId());
        activityService.logActivity(card, user, "ADD_CHECKLIST", "added checklist \"" + request.name() + "\"");
        
        return ChecklistResponse.builder()
                .id(checklist.getId())
                .cardId(card.getId())
                .name(checklist.getName())
                .position(checklist.getPosition())
                .items(Collections.emptyList())
                .build();
    }

    @Override
    @Transactional
    public ChecklistItemResponse createChecklistItem(Long checklistId, CreateChecklistItemRequest request, UserPrincipal currentUser) {
        Checklist checklist = checklistRepository.findById(checklistId)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist", checklistId));
        validateWriteAccess(checklist.getCard(), currentUser.getId());

        int nextPosition = checklist.getItems().size();
        ChecklistItem item = ChecklistItem.builder()
                .checklist(checklist)
                .content(request.content())
                .position(nextPosition)
                .build();
        
        item = checklistItemRepository.save(item);
        
        User user = findUserOrThrow(currentUser.getId());
        activityService.logActivity(checklist.getCard(), user, "ADD_CHECKLIST_ITEM", "added \"" + item.getContent() + "\" to " + checklist.getName());
        
        return ChecklistItemResponse.builder()
                .id(item.getId())
                .checklistId(checklist.getId())
                .content(item.getContent())
                .isCompleted(item.getIsCompleted())
                .position(item.getPosition())
                .build();
    }

    @Override
    @Transactional
    public ChecklistItemResponse updateChecklistItem(Long itemId, UpdateChecklistItemRequest request, UserPrincipal currentUser) {
        ChecklistItem item = checklistItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("ChecklistItem", itemId));
        validateWriteAccess(item.getChecklist().getCard(), currentUser.getId());

        if (request.content() != null) item.setContent(request.content());
        if (request.isCompleted() != null) item.setIsCompleted(request.isCompleted());
        if (request.position() != null) item.setPosition(request.position());

        item = checklistItemRepository.save(item);

        return ChecklistItemResponse.builder()
                .id(item.getId())
                .checklistId(item.getChecklist().getId())
                .content(item.getContent())
                .isCompleted(item.getIsCompleted())
                .position(item.getPosition())
                .build();
    }

    // ─── Label Management ───────────────────────────────

    @Override
    @Transactional
    public LabelResponse createLabel(Long boardId, CreateLabelRequest request, UserPrincipal currentUser) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board", boardId));
        validateWriteAccess(board.getWorkspace().getId(), currentUser.getId());

        Label label = Label.builder()
                .board(board)
                .name(request.name())
                .color(request.color())
                .build();
        
        label = labelRepository.save(label);

        return LabelResponse.builder()
                .id(label.getId())
                .boardId(label.getBoard().getId())
                .name(label.getName())
                .color(label.getColor())
                .build();
    }

    @Override
    public List<LabelResponse> getBoardLabels(Long boardId, UserPrincipal currentUser) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board", boardId));
        validateMembership(board.getWorkspace().getId(), currentUser.getId());

        return labelRepository.findByBoardId(boardId).stream()
                .map(l -> LabelResponse.builder()
                        .id(l.getId())
                        .boardId(boardId)
                        .name(l.getName())
                        .color(l.getColor())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addLabelToCard(Long cardId, Long labelId, UserPrincipal currentUser) {
        Card card = findCardOrThrow(cardId);
        validateWriteAccess(card, currentUser.getId());

        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new ResourceNotFoundException("Label", labelId));

        if (!label.getBoard().getId().equals(card.getTaskList().getBoard().getId())) {
            throw new UnauthorizedException("Label does not belong to this board");
        }

        card.getLabels().add(label);
        cardRepository.save(card);

        User user = findUserOrThrow(currentUser.getId());
        activityService.logActivity(card, user, "ADD_LABEL", "added label \"" + label.getName() + "\"");
        log.info("Label {} added to card {}", labelId, cardId);
    }

    @Override
    @Transactional
    public void removeLabelFromCard(Long cardId, Long labelId, UserPrincipal currentUser) {
        Card card = findCardOrThrow(cardId);
        validateWriteAccess(card, currentUser.getId());

        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new ResourceNotFoundException("Label", labelId));

        card.getLabels().remove(label);
        cardRepository.save(card);

        User user = findUserOrThrow(currentUser.getId());
        activityService.logActivity(card, user, "REMOVE_LABEL", "removed label \"" + label.getName() + "\"");
        log.info("Label {} removed from card {}", labelId, cardId);
    }

    // ─── Member Assignment ──────────────────────────────

    @Override
    @Transactional
    public void assignMember(Long cardId, Long userId, UserPrincipal currentUser) {
        Card card = findCardOrThrow(cardId);
        validateWriteAccess(card, currentUser.getId());

        User member = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        User actor = findUserOrThrow(currentUser.getId());

        // Validate user is a workspace member
        Long workspaceId = card.getTaskList().getBoard().getWorkspace().getId();
        if (!memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new UnauthorizedException("User is not a member of this workspace");
        }

        card.getMembers().add(member);
        cardRepository.save(card);
        
        activityService.logActivity(card, actor, "ASSIGN_MEMBER", "added " + member.getUsername() + " to this card");
        
        notificationService.createNotification(
            member, 
            actor, 
            "ASSIGNED_TO_CARD", 
            "CARD", 
            card.getId(), 
            String.format("%s assigned you to the card \"%s\"", actor.getUsername(), card.getTitle())
        );
        
        log.info("Member {} assigned to card {}", userId, cardId);
    }

    @Override
    @Transactional
    public void unassignMember(Long cardId, Long userId, UserPrincipal currentUser) {
        Card card = findCardOrThrow(cardId);
        validateWriteAccess(card, currentUser.getId());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        card.getMembers().remove(user);
        cardRepository.save(card);
        log.info("User {} unassigned from card {}", userId, cardId);
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
        validateMembership(card.getTaskList().getBoard().getWorkspace().getId(), userId);
    }

    private void validateMembership(Long workspaceId, Long userId) {
        if (!memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new UnauthorizedException("You are not a member of this workspace");
        }
    }

    private void validateWriteAccess(Card card, Long userId) {
        validateWriteAccess(card.getTaskList().getBoard().getWorkspace().getId(), userId);
    }

    private void validateWriteAccess(TaskList taskList, Long userId) {
        validateWriteAccess(taskList.getBoard().getWorkspace().getId(), userId);
    }

    private void validateWriteAccess(Long workspaceId, Long userId) {
        boolean hasAccess = memberRepository.existsByWorkspaceIdAndUserIdAndRoleIn(
                workspaceId, userId, List.of(com.okabe.entity.enums.Role.OWNER, com.okabe.entity.enums.Role.ADMIN, com.okabe.entity.enums.Role.MEMBER));
        if (!hasAccess) {
            throw new UnauthorizedException("You do not have permission to perform this action");
        }
    }

    private CardResponse toCardResponse(Card card) {
        List<LabelResponse> labelResponses = card.getLabels().stream()
                .map(l -> LabelResponse.builder()
                        .id(l.getId())
                        .boardId(l.getBoard().getId())
                        .name(l.getName())
                        .color(l.getColor())
                        .build())
                .collect(Collectors.toList());

        List<ChecklistResponse> checklistResponses = card.getChecklists().stream()
                .map(c -> ChecklistResponse.builder()
                        .id(c.getId())
                        .cardId(card.getId())
                        .name(c.getName())
                        .position(c.getPosition())
                        .items(c.getItems().stream()
                                .map(i -> ChecklistItemResponse.builder()
                                        .id(i.getId())
                                        .checklistId(c.getId())
                                        .content(i.getContent())
                                        .isCompleted(i.getIsCompleted())
                                        .position(i.getPosition())
                                        .build())
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());

        List<UserResponse> memberResponses = card.getMembers().stream()
                .map(m -> UserResponse.builder()
                        .id(m.getId())
                        .username(m.getUsername())
                        .email(m.getEmail())
                        .avatarUrl(m.getAvatarUrl())
                        .build())
                .collect(Collectors.toList());

        List<AttachmentResponse> attachmentResponses = card.getAttachments().stream()
                .map(a -> AttachmentResponse.builder()
                        .id(a.getId())
                        .cardId(card.getId())
                        .uploadedById(a.getUploadedBy().getId())
                        .uploadedByUsername(a.getUploadedBy().getUsername())
                        .filename(a.getFilename())
                        .url(a.getStorageKey())
                        .fileSize(a.getFileSize())
                        .mimeType(a.getMimeType())
                        .createdAt(a.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

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
                .labels(labelResponses)
                .checklists(checklistResponses)
                .members(memberResponses)
                .attachments(attachmentResponses)
                .build();
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
