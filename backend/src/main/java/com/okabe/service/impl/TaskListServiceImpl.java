package com.okabe.service.impl;

import com.okabe.dto.request.CreateListRequest;
import com.okabe.dto.request.ReorderListRequest;
import com.okabe.dto.request.UpdateListRequest;
import com.okabe.dto.response.*;
import com.okabe.entity.Board;
import com.okabe.entity.Card;
import com.okabe.entity.TaskList;
import com.okabe.exception.ResourceNotFoundException;
import com.okabe.exception.UnauthorizedException;
import com.okabe.repository.*;
import com.okabe.security.UserPrincipal;
import com.okabe.service.TaskListService;
import com.okabe.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskListServiceImpl implements TaskListService {

    private final TaskListRepository taskListRepository;
    private final BoardRepository boardRepository;
    private final CardRepository cardRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final WebSocketService webSocketService;

    @Override
    public List<ListResponse> getListsByBoard(Long boardId, UserPrincipal currentUser) {
        Board board = findBoardOrThrow(boardId);
        validateMembership(board, currentUser.getId());

        return taskListRepository.findByBoardIdAndIsArchivedFalseOrderByPositionAsc(boardId)
                .stream().map(this::toListResponse).toList();
    }

    @Override
    @Transactional
    public ListResponse createList(Long boardId, CreateListRequest request, UserPrincipal currentUser) {
        Board board = findBoardOrThrow(boardId);
        validateWriteAccess(board, currentUser.getId());

        int nextPosition = taskListRepository.countByBoardIdAndIsArchivedFalse(boardId);

        TaskList taskList = TaskList.builder()
                .board(board)
                .name(request.name())
                .position(nextPosition)
                .build();

        taskList = taskListRepository.save(taskList);
        log.info("List created: {} in board {}", taskList.getName(), boardId);
        
        ListResponse response = toListResponse(taskList);
        webSocketService.broadcastToBoard(boardId, "LIST_CREATED", response, currentUser.getId());
        
        return response;
    }

    @Override
    @Transactional
    public ListResponse updateList(Long listId, UpdateListRequest request, UserPrincipal currentUser) {
        TaskList taskList = findListOrThrow(listId);
        validateWriteAccess(taskList.getBoard(), currentUser.getId());

        if (request.name() != null) taskList.setName(request.name());
        if (request.isArchived() != null) taskList.setIsArchived(request.isArchived());

        taskList = taskListRepository.save(taskList);
        ListResponse response = toListResponse(taskList);
        webSocketService.broadcastToBoard(taskList.getBoard().getId(), "LIST_UPDATED", response, currentUser.getId());
        
        return response;
    }

    @Override
    @Transactional
    public void reorderLists(Long boardId, ReorderListRequest request, UserPrincipal currentUser) {
        Board board = findBoardOrThrow(boardId);
        validateWriteAccess(board, currentUser.getId());

        List<Long> orderedIds = request.orderedIds();
        for (int i = 0; i < orderedIds.size(); i++) {
            TaskList taskList = findListOrThrow(orderedIds.get(i));
            taskList.setPosition(i);
            taskListRepository.save(taskList);
        }
        log.info("Lists reordered in board {}", boardId);
        webSocketService.broadcastToBoard(boardId, "LIST_REORDERED", orderedIds, currentUser.getId());
    }

    @Override
    @Transactional
    public void deleteList(Long listId, UserPrincipal currentUser) {
        TaskList taskList = findListOrThrow(listId);
        Long boardId = taskList.getBoard().getId();
        taskListRepository.delete(taskList);
        log.info("List deleted: {}", listId);
        webSocketService.broadcastToBoard(boardId, "LIST_DELETED", listId, currentUser.getId());
    }

    @Override
    @Transactional
    public ListResponse archiveList(Long listId, UserPrincipal currentUser) {
        TaskList taskList = findListOrThrow(listId);
        validateAdminAccess(taskList.getBoard(), currentUser.getId());

        taskList.setIsArchived(true);
        taskList = taskListRepository.save(taskList);

        // Cascade archive to cards
        List<Card> cards = cardRepository.findByTaskListIdAndIsArchivedFalseOrderByPositionAsc(listId);
        for (Card card : cards) {
            card.setIsArchived(true);
        }
        cardRepository.saveAll(cards);

        log.info("List and its cards archived: {}", listId);
        ListResponse response = toListResponse(taskList);
        webSocketService.broadcastToBoard(taskList.getBoard().getId(), "LIST_ARCHIVED", listId, currentUser.getId());
        return response;
    }

    @Override
    @Transactional
    public ListResponse restoreList(Long listId, UserPrincipal currentUser) {
        TaskList taskList = findListOrThrow(listId);
        validateAdminAccess(taskList.getBoard(), currentUser.getId());

        // Reset position to end
        TaskList lastList = taskListRepository.findTopByBoardIdAndIsArchivedFalseOrderByPositionDesc(taskList.getBoard().getId());
        int nextPosition = lastList == null ? 0 : lastList.getPosition() + 1;

        taskList.setIsArchived(false);
        taskList.setPosition(nextPosition);
        taskList = taskListRepository.save(taskList);

        // Restore cards
        List<Card> archivedCards = cardRepository.findByTaskListIdAndIsArchivedTrueOrderByPositionAsc(listId);
        for (Card card : archivedCards) {
            card.setIsArchived(false);
        }
        cardRepository.saveAll(archivedCards);

        log.info("List and its cards restored: {}", listId);
        ListResponse response = toListResponse(taskList);
        webSocketService.broadcastToBoard(taskList.getBoard().getId(), "LIST_RESTORED", response, currentUser.getId());
        return response;
    }

    @Override
    public List<ListResponse> getArchivedLists(Long boardId, UserPrincipal currentUser) {
        Board board = findBoardOrThrow(boardId);
        validateMembership(board, currentUser.getId());
        return taskListRepository.findByBoardIdAndIsArchivedTrueOrderByPositionAsc(boardId)
                .stream().map(this::toListResponse).toList();
    }

    // ─── Helpers ────────────────────────────────────────

    private Board findBoardOrThrow(Long id) {
        return boardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Board", id));
    }

    private TaskList findListOrThrow(Long id) {
        return taskListRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("List", id));
    }

    private void validateMembership(Board board, Long userId) {
        if (!memberRepository.existsByWorkspaceIdAndUserId(board.getWorkspace().getId(), userId)) {
            throw new UnauthorizedException("You are not a member of this workspace");
        }
    }

    private void validateWriteAccess(Board board, Long userId) {
        boolean hasAccess = memberRepository.existsByWorkspaceIdAndUserIdAndRoleIn(
                board.getWorkspace().getId(), userId, List.of(com.okabe.entity.enums.Role.OWNER, com.okabe.entity.enums.Role.ADMIN, com.okabe.entity.enums.Role.MEMBER));
        if (!hasAccess) {
            throw new UnauthorizedException("You do not have permission to perform this action");
        }
    }

    private void validateAdminAccess(Board board, Long userId) {
        boolean hasAccess = memberRepository.existsByWorkspaceIdAndUserIdAndRoleIn(
                board.getWorkspace().getId(), userId, List.of(com.okabe.entity.enums.Role.OWNER, com.okabe.entity.enums.Role.ADMIN));
        if (!hasAccess) {
            throw new UnauthorizedException("Only OWNER or ADMIN can perform this action");
        }
    }

    private ListResponse toListResponse(TaskList taskList) {
        List<Card> cards = cardRepository
                .findByTaskListIdAndIsArchivedFalseOrderByPositionAsc(taskList.getId());

        return ListResponse.builder()
                .id(taskList.getId())
                .boardId(taskList.getBoard().getId())
                .name(taskList.getName())
                .position(taskList.getPosition())
                .cards(cards.stream().map(this::toCardResponse).toList())
                .build();
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
                .members(card.getMembers().stream()
                        .map(m -> UserResponse.builder()
                                .id(m.getId())
                                .username(m.getUsername())
                                .email(m.getEmail())
                                .avatarUrl(m.getAvatarUrl())
                                .build())
                        .collect(Collectors.toList()))
                .attachments(card.getAttachments().stream()
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
                        .collect(Collectors.toList()))
                .build();
    }
}
