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
        Board board = findBoardOrThrow(boardId); // Tìm bảng theo ID
        validateMembership(board, currentUser.getId()); // Kiểm tra quyền thành viên

        return taskListRepository.findByBoardIdAndIsArchivedFalseOrderByPositionAsc(boardId) // Lấy danh sách cột
                .stream().map(this::toListResponse).toList(); // Chuyển đổi và trả về
    }

    @Override
    @Transactional
    public ListResponse createList(Long boardId, CreateListRequest request, UserPrincipal currentUser) {
        Board board = findBoardOrThrow(boardId); // Tìm bảng
        validateWriteAccess(board, currentUser.getId()); // Kiểm tra quyền ghi

        int nextPosition = taskListRepository.countByBoardIdAndIsArchivedFalse(boardId); // Lấy vị trí tiếp theo

        TaskList taskList = TaskList.builder()
                .board(board) // Gán bảng
                .name(request.name()) // Gán tên cột
                .position(nextPosition) // Gán vị trí
                .build(); // Xây dựng TaskList

        taskList = taskListRepository.save(taskList); // Lưu cột mới
        log.info("List created: {} in board {}", taskList.getName(), boardId); // Ghi log
        
        ListResponse response = toListResponse(taskList); // Chuyển đổi sang response
        webSocketService.broadcastToBoard(boardId, "LIST_CREATED", response, currentUser.getId()); // Broadcast WebSocket
        
        return response; // Trả về phản hồi
    }

    @Override
    @Transactional
    public ListResponse updateList(Long listId, UpdateListRequest request, UserPrincipal currentUser) {
        TaskList taskList = findListOrThrow(listId); // Tìm cột
        validateWriteAccess(taskList.getBoard(), currentUser.getId()); // Kiểm tra quyền

        if (request.name() != null) taskList.setName(request.name()); // Cập nhật tên
        if (request.isArchived() != null) taskList.setIsArchived(request.isArchived()); // Cập nhật trạng thái archive

        taskList = taskListRepository.save(taskList); // Lưu thay đổi
        ListResponse response = toListResponse(taskList); // Chuyển đổi
        webSocketService.broadcastToBoard(taskList.getBoard().getId(), "LIST_UPDATED", response, currentUser.getId()); // Broadcast
        
        return response; // Trả về phản hồi
    }

    @Override
    @Transactional
    public void reorderLists(Long boardId, ReorderListRequest request, UserPrincipal currentUser) {
        Board board = findBoardOrThrow(boardId); // Tìm bảng
        validateWriteAccess(board, currentUser.getId()); // Kiểm tra quyền

        List<Long> orderedIds = request.orderedIds(); // Lấy danh sách ID đã sắp xếp
        for (int i = 0; i < orderedIds.size(); i++) { // Duyệt danh sách
            TaskList taskList = findListOrThrow(orderedIds.get(i)); // Tìm cột
            taskList.setPosition(i); // Cập nhật vị trí
            taskListRepository.save(taskList); // Lưu thay đổi
        }
        log.info("Lists reordered in board {}", boardId); // Ghi log
        webSocketService.broadcastToBoard(boardId, "LIST_REORDERED", orderedIds, currentUser.getId()); // Broadcast
    }

    @Override
    @Transactional
    public void deleteList(Long listId, UserPrincipal currentUser) {
        TaskList taskList = findListOrThrow(listId); // Tìm cột
        Long boardId = taskList.getBoard().getId(); // Lấy board ID
        taskListRepository.delete(taskList); // Xóa cột
        log.info("List deleted: {}", listId); // Ghi log
        webSocketService.broadcastToBoard(boardId, "LIST_DELETED", listId, currentUser.getId()); // Broadcast
    }

    @Override
    @Transactional
    public ListResponse archiveList(Long listId, UserPrincipal currentUser) {
        TaskList taskList = findListOrThrow(listId); // Tìm cột
        validateWriteAccess(taskList.getBoard(), currentUser.getId()); // Kiểm tra quyền

        taskList.setIsArchived(true); // Đánh dấu archived
        taskList = taskListRepository.save(taskList); // Lưu thay đổi

        // Cascade archive to cards
        List<Card> cards = cardRepository.findByTaskListIdAndIsArchivedFalseOrderByPositionAsc(listId); // Lấy card chưa archive
        for (Card card : cards) { // Duyệt từng card
            card.setIsArchived(true); // Đánh dấu archived
        }
        cardRepository.saveAll(cards); // Lưu tất cả card

        log.info("List and its cards archived: {}", listId); // Ghi log
        ListResponse response = toListResponse(taskList); // Chuyển đổi
        webSocketService.broadcastToBoard(taskList.getBoard().getId(), "LIST_ARCHIVED", listId, currentUser.getId()); // Broadcast
        return response; // Trả về phản hồi
    }

    @Override
    @Transactional
    public ListResponse restoreList(Long listId, UserPrincipal currentUser) {
        TaskList taskList = findListOrThrow(listId); // Tìm cột
        validateWriteAccess(taskList.getBoard(), currentUser.getId()); // Kiểm tra quyền

        // Reset position to end
        TaskList lastList = taskListRepository.findTopByBoardIdAndIsArchivedFalseOrderByPositionDesc(taskList.getBoard().getId()); // Tìm cột cuối
        int nextPosition = lastList == null ? 0 : lastList.getPosition() + 1; // Tính vị trí tiếp theo

        taskList.setIsArchived(false); // Bỏ archive
        taskList.setPosition(nextPosition); // Đặt vị trí mới
        taskList = taskListRepository.save(taskList); // Lưu thay đổi

        // Restore cards
        List<Card> archivedCards = cardRepository.findByTaskListIdAndIsArchivedTrueOrderByPositionAsc(listId); // Lấy card đã archive
        for (Card card : archivedCards) { // Duyệt từng card
            card.setIsArchived(false); // Bỏ archive
        }
        cardRepository.saveAll(archivedCards); // Lưu tất cả

        log.info("List and its cards restored: {}", listId); // Ghi log
        ListResponse response = toListResponse(taskList); // Chuyển đổi
        webSocketService.broadcastToBoard(taskList.getBoard().getId(), "LIST_RESTORED", response, currentUser.getId()); // Broadcast
        return response; // Trả về phản hồi
    }

    @Override
    public List<ListResponse> getArchivedLists(Long boardId, UserPrincipal currentUser) {
        Board board = findBoardOrThrow(boardId); // Tìm bảng
        validateMembership(board, currentUser.getId()); // Kiểm tra quyền
        return taskListRepository.findByBoardIdAndIsArchivedTrueOrderByPositionAsc(boardId) // Lấy cột đã archive
                .stream().map(this::toListResponse).toList(); // Chuyển đổi và trả về
    }

    // ─── Helpers ────────────────────────────────────────

    private Board findBoardOrThrow(Long id) {
        return boardRepository.findById(id) // Tìm bảng theo ID
                .orElseThrow(() -> new ResourceNotFoundException("Board", id)); // Ném lỗi nếu không tìm thấy
    }

    private TaskList findListOrThrow(Long id) {
        return taskListRepository.findById(id) // Tìm cột theo ID
                .orElseThrow(() -> new ResourceNotFoundException("List", id)); // Ném lỗi nếu không tìm thấy
    }

    private void validateMembership(Board board, Long userId) {
        if (!memberRepository.existsByWorkspaceIdAndUserId(board.getWorkspace().getId(), userId)) { // Kiểm tra thành viên
            throw new UnauthorizedException("You are not a member of this workspace"); // Ném lỗi
        }
    }

    private void validateWriteAccess(Board board, Long userId) {
        boolean hasAccess = memberRepository.existsByWorkspaceIdAndUserIdAndRoleIn( // Kiểm tra quyền ghi
                board.getWorkspace().getId(), userId, List.of(com.okabe.entity.enums.Role.OWNER, com.okabe.entity.enums.Role.ADMIN, com.okabe.entity.enums.Role.MEMBER));
        if (!hasAccess) { // Nếu không có quyền
            throw new UnauthorizedException("You do not have permission to perform this action"); // Ném lỗi
        }
    }

    private void validateAdminAccess(Board board, Long userId) {
        boolean hasAccess = memberRepository.existsByWorkspaceIdAndUserIdAndRoleIn( // Kiểm tra quyền admin
                board.getWorkspace().getId(), userId, List.of(com.okabe.entity.enums.Role.OWNER, com.okabe.entity.enums.Role.ADMIN));
        if (!hasAccess) { // Nếu không có quyền
            throw new UnauthorizedException("Only OWNER or ADMIN can perform this action"); // Ném lỗi
        }
    }

    private ListResponse toListResponse(TaskList taskList) {
        List<Card> cards = cardRepository // Lấy card trong cột
                .findByTaskListIdAndIsArchivedFalseOrderByPositionAsc(taskList.getId());

        return ListResponse.builder()
                .id(taskList.getId()) // Gán ID cột
                .boardId(taskList.getBoard().getId()) // Gán ID bảng
                .name(taskList.getName()) // Gán tên cột
                .position(taskList.getPosition()) // Gán vị trí
                .cards(cards.stream().map(this::toCardResponse).toList()) // Gán danh sách card
                .build(); // Xây dựng ListResponse
    }

    private CardResponse toCardResponse(Card card) {
        List<LabelResponse> labelResponses = card.getLabels().stream() // Chuyển đổi labels
                .map(l -> LabelResponse.builder()
                        .id(l.getId()) // Gán ID label
                        .boardId(l.getBoard().getId()) // Gán ID bảng
                        .name(l.getName()) // Gán tên
                        .color(l.getColor()) // Gán màu
                        .build()) // Xây dựng LabelResponse
                .collect(Collectors.toList()); // Thu thập thành danh sách

        List<ChecklistResponse> checklistResponses = card.getChecklists().stream() // Chuyển đổi checklists
                .map(c -> ChecklistResponse.builder()
                        .id(c.getId()) // Gán ID
                        .cardId(card.getId()) // Gán ID thẻ
                        .name(c.getName()) // Gán tên
                        .position(c.getPosition()) // Gán vị trí
                        .items(c.getItems().stream() // Chuyển đổi items
                                .map(i -> ChecklistItemResponse.builder()
                                        .id(i.getId()) // Gán ID item
                                        .checklistId(c.getId()) // Gán ID checklist
                                        .content(i.getContent()) // Gán nội dung
                                        .isCompleted(i.getIsCompleted()) // Gán trạng thái
                                        .position(i.getPosition()) // Gán vị trí
                                        .build()) // Xây dựng ChecklistItemResponse
                                .collect(Collectors.toList())) // Thu thập items
                        .build()) // Xây dựng ChecklistResponse
                .collect(Collectors.toList()); // Thu thập checklists

        return CardResponse.builder()
                .id(card.getId()) // Gán ID card
                .listId(card.getTaskList().getId()) // Gán ID list
                .title(card.getTitle()) // Gán tiêu đề
                .description(card.getDescription()) // Gán mô tả
                .position(card.getPosition()) // Gán vị trí
                .dueDate(card.getDueDate()) // Gán hạn chót
                .priority(card.getPriority().name()) // Gán độ ưu tiên
                .isArchived(card.getIsArchived()) // Gán trạng thái archive
                .totalFocusMinutes(card.getTotalFocusMinutes()) // Gán tổng phút focus
                .createdById(card.getCreatedBy().getId()) // Gán ID người tạo
                .createdByName(card.getCreatedBy().getUsername()) // Gán tên người tạo
                .createdAt(card.getCreatedAt()) // Gán thời gian tạo
                .labels(labelResponses) // Gán labels
                .checklists(checklistResponses) // Gán checklists
                .members(card.getMembers().stream() // Chuyển đổi members
                        .map(m -> UserResponse.builder()
                                .id(m.getId()) // Gán ID
                                .username(m.getUsername()) // Gán tên
                                .email(m.getEmail()) // Gán email
                                .avatarUrl(m.getAvatarUrl()) // Gán avatar
                                .build()) // Xây dựng UserResponse
                        .collect(Collectors.toList())) // Thu thập members
                .attachments(card.getAttachments().stream() // Chuyển đổi attachments
                        .map(a -> AttachmentResponse.builder()
                                .id(a.getId()) // Gán ID
                                .cardId(card.getId()) // Gán ID thẻ
                                .uploadedById(a.getUploadedBy().getId()) // Gán ID người tải
                                .uploadedByUsername(a.getUploadedBy().getUsername()) // Gán tên người tải
                                .filename(a.getFilename()) // Gán tên file
                                .url(a.getStorageKey()) // Gán URL
                                .fileSize(a.getFileSize()) // Gán kích thước
                                .mimeType(a.getMimeType()) // Gán loại MIME
                                .createdAt(a.getCreatedAt()) // Gán thời gian tạo
                                .build()) // Xây dựng AttachmentResponse
                        .collect(Collectors.toList())) // Thu thập attachments
                .build(); // Xây dựng CardResponse
    }
}
