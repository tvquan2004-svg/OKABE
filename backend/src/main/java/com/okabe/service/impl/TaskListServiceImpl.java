package com.okabe.service.impl;

import com.okabe.dto.request.CreateListRequest;
import com.okabe.dto.request.ReorderListRequest;
import com.okabe.dto.request.UpdateListRequest;
import com.okabe.dto.response.CardResponse;
import com.okabe.dto.response.ListResponse;
import com.okabe.entity.Board;
import com.okabe.entity.Card;
import com.okabe.entity.TaskList;
import com.okabe.exception.ResourceNotFoundException;
import com.okabe.exception.UnauthorizedException;
import com.okabe.repository.*;
import com.okabe.security.UserPrincipal;
import com.okabe.service.TaskListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskListServiceImpl implements TaskListService {

    private final TaskListRepository taskListRepository;
    private final BoardRepository boardRepository;
    private final CardRepository cardRepository;
    private final WorkspaceMemberRepository memberRepository;

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
        validateMembership(board, currentUser.getId());

        int nextPosition = taskListRepository.countByBoardIdAndIsArchivedFalse(boardId);

        TaskList taskList = TaskList.builder()
                .board(board)
                .name(request.name())
                .position(nextPosition)
                .build();

        taskList = taskListRepository.save(taskList);
        log.info("List created: {} in board {}", taskList.getName(), boardId);
        return toListResponse(taskList);
    }

    @Override
    @Transactional
    public ListResponse updateList(Long listId, UpdateListRequest request, UserPrincipal currentUser) {
        TaskList taskList = findListOrThrow(listId);
        validateMembership(taskList.getBoard(), currentUser.getId());

        if (request.name() != null) taskList.setName(request.name());
        if (request.isArchived() != null) taskList.setIsArchived(request.isArchived());

        taskList = taskListRepository.save(taskList);
        return toListResponse(taskList);
    }

    @Override
    @Transactional
    public void reorderLists(Long boardId, ReorderListRequest request, UserPrincipal currentUser) {
        Board board = findBoardOrThrow(boardId);
        validateMembership(board, currentUser.getId());

        List<Long> orderedIds = request.orderedIds();
        for (int i = 0; i < orderedIds.size(); i++) {
            TaskList taskList = findListOrThrow(orderedIds.get(i));
            taskList.setPosition(i);
            taskListRepository.save(taskList);
        }
        log.info("Lists reordered in board {}", boardId);
    }

    @Override
    @Transactional
    public void deleteList(Long listId, UserPrincipal currentUser) {
        TaskList taskList = findListOrThrow(listId);
        validateMembership(taskList.getBoard(), currentUser.getId());
        taskListRepository.delete(taskList);
        log.info("List deleted: {}", listId);
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
