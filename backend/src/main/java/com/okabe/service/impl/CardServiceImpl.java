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
import com.okabe.service.EmailNotificationService;
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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
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
    private final EmailNotificationService emailNotificationService;
    private final ObjectMapper objectMapper;

    @Override
    public Page<CardResponse> searchCards(Long boardId, CardSearchRequest request, UserPrincipal currentUser) {
        // Validate board exists and user has access
        Board board = boardRepository.findById(boardId) // Tìm bảng theo ID
                .orElseThrow(() -> new ResourceNotFoundException("Board", boardId)); // Ném lỗi nếu không tìm thấy
        validateMembership(board.getWorkspace().getId(), currentUser.getId()); // Kiểm tra quyền thành viên

        Sort sort = Sort.by(Sort.Direction.ASC, "dueDate").and(Sort.by(Sort.Direction.ASC, "position")); // Sắp xếp theo ngày và vị trí
        PageRequest pageRequest = PageRequest.of(request.getPage(), request.getSize(), sort); // Tạo page request

        Specification<Card> spec = CardSpecification.filterByRequest(boardId, request); // Xây dựng specification tìm kiếm
        
        return cardRepository.findAll(spec, pageRequest) // Thực hiện truy vấn phân trang
                .map(this::toCardResponse); // Chuyển đổi sang CardResponse
    }

    @Override
    public CardResponse getCard(Long cardId, UserPrincipal currentUser) {
        Card card = findCardOrThrow(cardId); // Tìm card theo ID
        validateMembership(card, currentUser.getId()); // Kiểm tra quyền
        return toCardResponse(card); // Trả về CardResponse
    }

    @Override
    @Transactional
    public CardResponse createCard(Long listId, CreateCardRequest request, UserPrincipal currentUser) {
        TaskList taskList = findListOrThrow(listId); // Tìm cột theo ID
        validateWriteAccess(taskList, currentUser.getId()); // Kiểm tra quyền ghi

        User creator = userRepository.findById(currentUser.getId()) // Tìm người tạo
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUser.getId())); // Ném lỗi nếu không tìm thấy

        int nextPosition = cardRepository.countByTaskListIdAndIsArchivedFalse(listId); // Lấy vị trí tiếp theo

        Priority priority = Priority.MEDIUM; // Mặc định Medium
        if (request.priority() != null) { // Nếu có priority từ request
            try {
                priority = Priority.valueOf(request.priority().toUpperCase()); // Chuyển đổi từ string
            } catch (IllegalArgumentException ignored) {} // Bỏ qua nếu không hợp lệ
        }

        LocalDateTime dueDate = parseDateTime(request.dueDate()); // Parse ngày hết hạn
        LocalDateTime startDate = parseDateTime(request.startDate()); // Parse ngày bắt đầu

        Card card = Card.builder()
                .taskList(taskList) // Gán cột
                .title(request.title()) // Gán tiêu đề
                .description(request.description()) // Gán mô tả
                .position(nextPosition) // Gán vị trí
                .priority(priority) // Gán độ ưu tiên
                .dueDate(dueDate) // Gán hạn chót
                .startDate(startDate) // Gán ngày bắt đầu
                .createdBy(creator) // Gán người tạo
                .build(); // Xây dựng Card

        card = cardRepository.save(card); // Lưu card
        log.info("Card created: {} in list {}", card.getTitle(), listId); // Ghi log
        
        CardResponse response = toCardResponse(card); // Chuyển đổi
        webSocketService.broadcastToBoard(taskList.getBoard().getId(), "CARD_CREATED", response, currentUser.getId()); // Broadcast WebSocket
        
        return response; // Trả về phản hồi
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) return null; // Trả về null nếu chuỗi rỗng
        try {
            if (dateTimeStr.length() == 16) { // Nếu chỉ có ngày giờ không có giây
                return LocalDateTime.parse(dateTimeStr + ":00"); // Thêm :00 và parse
            }
            return LocalDateTime.parse(dateTimeStr); // Parse trực tiếp
        } catch (Exception e) {
            log.error("Failed to parse date time: {}", dateTimeStr); // Ghi log lỗi
            return null; // Trả về null nếu lỗi
        }
    }

    @Override
    @Transactional
    public CardResponse updateCard(Long cardId, UpdateCardRequest request, UserPrincipal currentUser) {
        Card card = findCardOrThrow(cardId); // Tìm card
        validateWriteAccess(card, currentUser.getId()); // Kiểm tra quyền

        User user = findUserOrThrow(currentUser.getId()); // Tìm người dùng

        if (request.title() != null && !request.title().equals(card.getTitle())) { // Nếu tiêu đề thay đổi
            activityService.logActivity(card, user, "UPDATE_CARD", "changed title to \"" + request.title() + "\""); // Ghi log hoạt động
            card.setTitle(request.title()); // Cập nhật tiêu đề
        }
        if (request.description() != null && !request.description().equals(card.getDescription())) { // Nếu mô tả thay đổi
            activityService.logActivity(card, user, "UPDATE_CARD", "updated description"); // Ghi log
            card.setDescription(request.description()); // Cập nhật mô tả
        }
        if (request.isArchived() != null && !request.isArchived().equals(card.getIsArchived())) { // Nếu trạng thái archive thay đổi
            activityService.logActivity(card, user, request.isArchived() ? "ARCHIVE_CARD" : "RESTORE_CARD", null); // Ghi log
            card.setIsArchived(request.isArchived()); // Cập nhật
        }
        if (request.priority() != null) { // Nếu có priority
            try {
                Priority newPriority = Priority.valueOf(request.priority().toUpperCase()); // Chuyển đổi
                if (newPriority != card.getPriority()) { // Nếu thay đổi
                    activityService.logActivity(card, user, "UPDATE_CARD", "changed priority to " + newPriority); // Ghi log
                    card.setPriority(newPriority); // Cập nhật
                }
            } catch (IllegalArgumentException ignored) {} // Bỏ qua nếu không hợp lệ
        }
        if (request.dueDate() != null) { // Nếu có due date
            LocalDateTime newDueDate = parseDateTime(request.dueDate()); // Parse ngày mới
            if (!java.util.Objects.equals(newDueDate, card.getDueDate())) { // Nếu thay đổi
                activityService.logActivity(card, user, "UPDATE_CARD", "updated due date"); // Ghi log
                card.setDueDate(newDueDate); // Cập nhật due date
                // Reset notification flag so scheduler re-notifies for the new due date
                card.setNotificationSent(false); // Reset cờ thông báo
                log.info("Card {} due date changed to {}, notification flag reset", card.getId(), newDueDate); // Ghi log
            }
        }
        if (request.startDate() != null) { // Nếu có start date
            LocalDateTime newStartDate = parseDateTime(request.startDate()); // Parse ngày mới
            if (!java.util.Objects.equals(newStartDate, card.getStartDate())) { // Nếu thay đổi
                activityService.logActivity(card, user, "UPDATE_CARD", "updated start date"); // Ghi log
                card.setStartDate(newStartDate); // Cập nhật start date
            }
        }

        card = cardRepository.save(card); // Lưu thay đổi
        CardResponse response = toCardResponse(card); // Chuyển đổi
        webSocketService.broadcastToBoard(card.getTaskList().getBoard().getId(), "CARD_UPDATED", response, currentUser.getId()); // Broadcast
        
        return response; // Trả về phản hồi
    }

    @Override
    @Transactional
    public CardResponse moveCard(Long cardId, MoveCardRequest request, UserPrincipal currentUser) {
        Card card = findCardOrThrow(cardId); // Tìm card theo ID
        validateWriteAccess(card, currentUser.getId()); // Kiểm tra quyền ghi

        TaskList targetList = findListOrThrow(request.targetListId()); // Tìm cột đích
        Board sourceBoard = card.getTaskList().getBoard(); // Lấy bảng nguồn
        Board targetBoard = targetList.getBoard(); // Lấy bảng đích

        // Validate write access to target workspace
        validateWriteAccess(targetBoard.getWorkspace().getId(), currentUser.getId()); // Kiểm tra quyền workspace đích

        User actor = findUserOrThrow(currentUser.getId()); // Tìm người thực hiện

        // 1. Remove from source list and reorder
        List<Card> sourceCards = cardRepository // Lấy card trong cột nguồn
                .findByTaskListIdAndIsArchivedFalseOrderByPositionAsc(card.getTaskList().getId());
        sourceCards.remove(card); // Xóa card khỏi danh sách nguồn
        for (int i = 0; i < sourceCards.size(); i++) { // Duyệt và cập nhật vị trí
            sourceCards.get(i).setPosition(i); // Đặt lại vị trí
        }
        cardRepository.saveAll(sourceCards); // Lưu tất cả thay đổi

        // 2. Handle cross-board specific logic
        if (!sourceBoard.getId().equals(targetBoard.getId())) { // Nếu di chuyển khác bảng
            // Clear labels as they are board-specific
            card.getLabels().clear(); // Xóa labels (vì labels thuộc về bảng)
            activityService.logActivity(card, actor, "MOVE_CARD",  // Ghi log di chuyển khác bảng
                "moved card from board \"" + sourceBoard.getName() + "\" to \"" + targetBoard.getName() + "\"");
        } else { // Nếu di chuyển trong cùng bảng
            activityService.logActivity(card, actor, "MOVE_CARD",  // Ghi log di chuyển trong bảng
                "moved card to list \"" + targetList.getName() + "\"");
        }

        // 3. Add to target list and reorder
        List<Card> targetCards = cardRepository // Lấy card trong cột đích
                .findByTaskListIdAndIsArchivedFalseOrderByPositionAsc(targetList.getId());
        
        // Use 0 as default position if not provided, or end of list
        int newPos = (request.position() != null) ? request.position() : targetCards.size(); // Vị trí mới
        newPos = Math.max(0, Math.min(newPos, targetCards.size())); // Giới hạn trong phạm vi

        boolean wasPreviouslyCompleted = isCompletedList(card.getTaskList().getName()); // Kiểm tra cột cũ có phải hoàn thành

        card.setTaskList(targetList); // Gán cột mới
        card.setPosition(newPos); // Đặt vị trí mới

        for (Card tc : targetCards) { // Dịch chuyển các card khác
            if (tc.getPosition() >= newPos) { // Nếu vị trí >= vị trí mới
                tc.setPosition(tc.getPosition() + 1); // Tăng vị trí lên 1
            }
        }
        
        cardRepository.saveAll(targetCards); // Lưu thay đổi vị trí
        card = cardRepository.save(card); // Lưu card

        // Notify dependent cards when blocker is moved to a Done list
        if (!wasPreviouslyCompleted && isCompletedList(targetList.getName())) { // Nếu vừa được đánh dấu hoàn thành
            List<Card> dependentCards = cardRepository.findDependentCards(cardId); // Tìm card phụ thuộc
            String message = "Dependency completed: \"" + card.getTitle() + "\""; // Tạo thông báo
            for (Card dc : dependentCards) { // Duyệt card phụ thuộc
                for (User member : dc.getMembers()) { // Duyệt thành viên
                    notificationService.createNotification(member, actor, // Tạo thông báo
                            "DEPENDENCY_COMPLETED", "CARD", dc.getId(), cardId, message);
                }
            }
            if (!dependentCards.isEmpty()) { // Nếu có card phụ thuộc
                log.info("Notified {} dependent cards of card {} completion", dependentCards.size(), cardId); // Ghi log
            }
        }

        log.info("Card {} moved from board {} to board {} list {} at position {}",  // Ghi log di chuyển
            cardId, sourceBoard.getId(), targetBoard.getId(), targetList.getId(), newPos);
        
        CardResponse response = toCardResponse(card); // Chuyển đổi
        webSocketService.broadcastToBoard(sourceBoard.getId(), "CARD_MOVED", response, currentUser.getId()); // Broadcast đến board nguồn
        if (!sourceBoard.getId().equals(targetBoard.getId())) { // Nếu khác board
            webSocketService.broadcastToBoard(targetBoard.getId(), "CARD_MOVED", response, currentUser.getId()); // Broadcast đến board đích
        }
        
        return response; // Trả về phản hồi
    }

    @Override
    @Transactional
    public void deleteCard(Long cardId, UserPrincipal currentUser) {
        Card card = findCardOrThrow(cardId); // Tìm card
        Long boardId = card.getTaskList().getBoard().getId(); // Lấy board ID
        cardRepository.delete(card); // Xóa card
        log.info("Card deleted: {}", cardId); // Ghi log
        webSocketService.broadcastToBoard(boardId, "CARD_DELETED", cardId, currentUser.getId()); // Broadcast
    }

    // ─── Checklist Management ──────────────────────────

    @Override
    @Transactional
    public ChecklistResponse createChecklist(Long cardId, CreateChecklistRequest request, UserPrincipal currentUser) {
        Card card = findCardOrThrow(cardId); // Tìm card
        validateWriteAccess(card, currentUser.getId()); // Kiểm tra quyền

        int nextPosition = card.getChecklists().size(); // Vị trí tiếp theo
        Checklist checklist = Checklist.builder()
                .card(card) // Gán card
                .name(request.name()) // Gán tên checklist
                .position(nextPosition) // Gán vị trí
                .build(); // Xây dựng Checklist
        
        Checklist savedChecklist = checklistRepository.save(checklist); // Lưu checklist

        User user = findUserOrThrow(currentUser.getId()); // Tìm người dùng
        activityService.logActivity(card, user, "ADD_CHECKLIST", "added checklist \"" + request.name() + "\""); // Ghi log
        
        return ChecklistResponse.builder()
                .id(savedChecklist.getId()) // Gán ID
                .cardId(card.getId()) // Gán card ID
                .name(savedChecklist.getName()) // Gán tên
                .position(savedChecklist.getPosition()) // Gán vị trí
                .items(Collections.emptyList()) // Danh sách rỗng
                .build(); // Xây dựng ChecklistResponse
    }

    @Override
    @Transactional
    public ChecklistResponse updateChecklist(Long checklistId, CreateChecklistRequest request, UserPrincipal currentUser) {
        Checklist checklist = checklistRepository.findById(checklistId) // Tìm checklist
                .orElseThrow(() -> new ResourceNotFoundException("Checklist", checklistId));
        validateWriteAccess(checklist.getCard(), currentUser.getId()); // Kiểm tra quyền

        String oldName = checklist.getName(); // Lưu tên cũ
        checklist.setName(request.name()); // Cập nhật tên
        Checklist savedChecklist = checklistRepository.save(checklist); // Lưu

        User user = findUserOrThrow(currentUser.getId()); // Tìm người dùng
        activityService.logActivity(checklist.getCard(), user, "UPDATE_CHECKLIST",  // Ghi log
                "renamed checklist from \"" + oldName + "\" to \"" + request.name() + "\"");

        return ChecklistResponse.builder()
                .id(savedChecklist.getId()) // Gán ID
                .cardId(savedChecklist.getCard().getId()) // Gán card ID
                .name(savedChecklist.getName()) // Gán tên
                .position(savedChecklist.getPosition()) // Gán vị trí
                .items(savedChecklist.getItems().stream() // Chuyển đổi items
                        .map(i -> ChecklistItemResponse.builder()
                                .id(i.getId()) // Gán ID item
                                .checklistId(savedChecklist.getId()) // Gán checklist ID
                                .content(i.getContent()) // Gán nội dung
                                .isCompleted(i.getIsCompleted()) // Gán trạng thái
                                .position(i.getPosition()) // Gán vị trí
                                .build())
                        .collect(Collectors.toList()))
                .build(); // Xây dựng ChecklistResponse
    }

    @Override
    @Transactional
    public void deleteChecklist(Long checklistId, UserPrincipal currentUser) {
        Checklist checklist = checklistRepository.findById(checklistId) // Tìm checklist
                .orElseThrow(() -> new ResourceNotFoundException("Checklist", checklistId));
        validateWriteAccess(checklist.getCard(), currentUser.getId()); // Kiểm tra quyền

        User user = findUserOrThrow(currentUser.getId()); // Tìm người dùng
        activityService.logActivity(checklist.getCard(), user, "DELETE_CHECKLIST", "removed checklist \"" + checklist.getName() + "\""); // Ghi log
        
        checklistRepository.delete(checklist); // Xóa checklist
    }

    @Override
    @Transactional
    public ChecklistItemResponse createChecklistItem(Long checklistId, CreateChecklistItemRequest request, UserPrincipal currentUser) {
        Checklist checklist = checklistRepository.findById(checklistId) // Tìm checklist
                .orElseThrow(() -> new ResourceNotFoundException("Checklist", checklistId));
        validateWriteAccess(checklist.getCard(), currentUser.getId()); // Kiểm tra quyền

        int nextPosition = checklist.getItems().size(); // Vị trí tiếp theo
        ChecklistItem item = ChecklistItem.builder()
                .checklist(checklist) // Gán checklist
                .content(request.content()) // Gán nội dung
                .position(nextPosition) // Gán vị trí
                .build(); // Xây dựng ChecklistItem
        
        ChecklistItem savedItem = checklistItemRepository.save(item); // Lưu item
        
        User user = findUserOrThrow(currentUser.getId()); // Tìm người dùng
        activityService.logActivity(checklist.getCard(), user, "ADD_CHECKLIST_ITEM", "added \"" + savedItem.getContent() + "\" to " + checklist.getName()); // Ghi log
        
        return ChecklistItemResponse.builder()
                .id(savedItem.getId()) // Gán ID
                .checklistId(checklist.getId()) // Gán checklist ID
                .content(savedItem.getContent()) // Gán nội dung
                .isCompleted(savedItem.getIsCompleted()) // Gán trạng thái
                .position(savedItem.getPosition()) // Gán vị trí
                .build(); // Xây dựng ChecklistItemResponse
    }

    @Override
    @Transactional
    public ChecklistItemResponse updateChecklistItem(Long itemId, UpdateChecklistItemRequest request, UserPrincipal currentUser) {
        ChecklistItem item = checklistItemRepository.findById(itemId) // Tìm item
                .orElseThrow(() -> new ResourceNotFoundException("ChecklistItem", itemId));
        validateWriteAccess(item.getChecklist().getCard(), currentUser.getId()); // Kiểm tra quyền

        if (request.content() != null) item.setContent(request.content()); // Cập nhật nội dung
        if (request.isCompleted() != null) item.setIsCompleted(request.isCompleted()); // Cập nhật trạng thái
        if (request.position() != null) item.setPosition(request.position()); // Cập nhật vị trí

        ChecklistItem savedItem = checklistItemRepository.save(item); // Lưu item

        return ChecklistItemResponse.builder()
                .id(savedItem.getId()) // Gán ID
                .checklistId(savedItem.getChecklist().getId()) // Gán checklist ID
                .content(savedItem.getContent()) // Gán nội dung
                .isCompleted(savedItem.getIsCompleted()) // Gán trạng thái
                .position(savedItem.getPosition()) // Gán vị trí
                .build(); // Xây dựng ChecklistItemResponse
    }

    @Override
    @Transactional
    public void deleteChecklistItem(Long itemId, UserPrincipal currentUser) {
        ChecklistItem item = checklistItemRepository.findById(itemId) // Tìm item
                .orElseThrow(() -> new ResourceNotFoundException("ChecklistItem", itemId));
        validateWriteAccess(item.getChecklist().getCard(), currentUser.getId()); // Kiểm tra quyền

        User user = findUserOrThrow(currentUser.getId()); // Tìm người dùng
        activityService.logActivity(item.getChecklist().getCard(), user, "DELETE_CHECKLIST_ITEM",  // Ghi log
                "removed \"" + item.getContent() + "\" from " + item.getChecklist().getName());
        
        checklistItemRepository.delete(item); // Xóa item
    }

    // ─── Label Management ───────────────────────────────

    @Override
    @Transactional
    public LabelResponse createLabel(Long boardId, CreateLabelRequest request, UserPrincipal currentUser) {
        Board board = boardRepository.findById(boardId) // Tìm bảng
                .orElseThrow(() -> new ResourceNotFoundException("Board", boardId));
        validateWriteAccess(board.getWorkspace().getId(), currentUser.getId()); // Kiểm tra quyền

        Label label = Label.builder()
                .board(board) // Gán bảng
                .name(request.name()) // Gán tên
                .color(request.color()) // Gán màu
                .build(); // Xây dựng Label
        
        label = labelRepository.save(label); // Lưu label

        return LabelResponse.builder()
                .id(label.getId()) // Gán ID
                .boardId(label.getBoard().getId()) // Gán board ID
                .name(label.getName()) // Gán tên
                .color(label.getColor()) // Gán màu
                .build(); // Xây dựng LabelResponse
    }

    @Override
    public List<LabelResponse> getBoardLabels(Long boardId, UserPrincipal currentUser) {
        Board board = boardRepository.findById(boardId) // Tìm bảng
                .orElseThrow(() -> new ResourceNotFoundException("Board", boardId));
        validateMembership(board.getWorkspace().getId(), currentUser.getId()); // Kiểm tra quyền

        return labelRepository.findByBoardId(boardId).stream() // Lấy labels của bảng
                .map(l -> LabelResponse.builder()
                        .id(l.getId()) // Gán ID
                        .boardId(boardId) // Gán board ID
                        .name(l.getName()) // Gán tên
                        .color(l.getColor()) // Gán màu
                        .build())
                .collect(Collectors.toList()); // Thu thập thành danh sách
    }

    @Override
    @Transactional
    public LabelResponse updateLabel(Long labelId, UpdateLabelRequest request, UserPrincipal currentUser) {
        Label label = labelRepository.findById(labelId) // Tìm label
                .orElseThrow(() -> new ResourceNotFoundException("Label", labelId));
        validateWriteAccess(label.getBoard().getWorkspace().getId(), currentUser.getId()); // Kiểm tra quyền

        if (request.name() != null) label.setName(request.name()); // Cập nhật tên
        if (request.color() != null) label.setColor(request.color()); // Cập nhật màu

        label = labelRepository.save(label); // Lưu label

        return LabelResponse.builder()
                .id(label.getId()) // Gán ID
                .boardId(label.getBoard().getId()) // Gán board ID
                .name(label.getName()) // Gán tên
                .color(label.getColor()) // Gán màu
                .build(); // Xây dựng LabelResponse
    }

    @Override
    @Transactional
    public void deleteLabel(Long labelId, UserPrincipal currentUser) {
        Label label = labelRepository.findById(labelId) // Tìm label
                .orElseThrow(() -> new ResourceNotFoundException("Label", labelId));
        validateWriteAccess(label.getBoard().getWorkspace().getId(), currentUser.getId()); // Kiểm tra quyền

        // Also remove association from all cards in this board
        List<Card> cards = cardRepository.findByTaskListBoardIdAndIsArchivedFalse(label.getBoard().getId()); // Lấy tất cả card trong board
        for (Card card : cards) { // Duyệt từng card
            card.getLabels().remove(label); // Xóa label khỏi card
        }
        cardRepository.saveAll(cards); // Lưu thay đổi

        labelRepository.delete(label); // Xóa label
        log.info("Label deleted: {}", labelId); // Ghi log
    }

    @Override
    @Transactional
    public void addLabelToCard(Long cardId, Long labelId, UserPrincipal currentUser) {
        Card card = findCardOrThrow(cardId); // Tìm card
        validateWriteAccess(card, currentUser.getId()); // Kiểm tra quyền

        Label label = labelRepository.findById(labelId) // Tìm label
                .orElseThrow(() -> new ResourceNotFoundException("Label", labelId));

        if (!label.getBoard().getId().equals(card.getTaskList().getBoard().getId())) { // Nếu label không thuộc board này
            throw new UnauthorizedException("Label does not belong to this board"); // Ném lỗi
        }

        card.getLabels().add(label); // Thêm label vào card
        cardRepository.save(card); // Lưu card

        User user = findUserOrThrow(currentUser.getId()); // Tìm người dùng
        activityService.logActivity(card, user, "ADD_LABEL", "added label \"" + label.getName() + "\""); // Ghi log
        log.info("Label {} added to card {}", labelId, cardId); // Ghi log
    }

    @Override
    @Transactional
    public void removeLabelFromCard(Long cardId, Long labelId, UserPrincipal currentUser) {
        Card card = findCardOrThrow(cardId); // Tìm card
        validateWriteAccess(card, currentUser.getId()); // Kiểm tra quyền

        Label label = labelRepository.findById(labelId) // Tìm label
                .orElseThrow(() -> new ResourceNotFoundException("Label", labelId));

        card.getLabels().remove(label); // Xóa label khỏi card
        cardRepository.save(card); // Lưu card

        User user = findUserOrThrow(currentUser.getId()); // Tìm người dùng
        activityService.logActivity(card, user, "REMOVE_LABEL", "removed label \"" + label.getName() + "\""); // Ghi log
        log.info("Label {} removed from card {}", labelId, cardId); // Ghi log
    }

    // ─── Member Assignment ──────────────────────────────

    @Override
    @Transactional
    public void assignMember(Long cardId, Long userId, UserPrincipal currentUser) {
        Card card = findCardOrThrow(cardId); // Tìm card
        validateWriteAccess(card, currentUser.getId()); // Kiểm tra quyền

        User member = userRepository.findById(userId) // Tìm thành viên
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        User actor = findUserOrThrow(currentUser.getId()); // Tìm người thực hiện

        // Validate user is a workspace member
        Long workspaceId = card.getTaskList().getBoard().getWorkspace().getId(); // Lấy workspace ID
        if (!memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) { // Kiểm tra là thành viên workspace
            throw new UnauthorizedException("User is not a member of this workspace"); // Ném lỗi
        }

        card.getMembers().add(member); // Thêm thành viên
        cardRepository.save(card); // Lưu card
        
        activityService.logActivity(card, actor, "ASSIGN_MEMBER", "added " + member.getUsername() + " to this card"); // Ghi log
        
        notificationService.createNotification( // Tạo thông báo
            member, // Người nhận
            actor, // Người gửi
            "ASSIGNED_TO_CARD", // Loại
            "CARD", // Thực thể
            card.getId(), // ID thẻ
            card.getTaskList().getBoard().getId(), // ID bảng
            String.format("%s đã giao cho bạn thẻ \"%s\"", actor.getUsername(), card.getTitle()) // Nội dung
        );

        emailNotificationService.sendCardAssignedEmail( // Gửi email
            actor, // Người gửi
            member, // Người nhận
            card.getTitle(), // Tiêu đề
            card.getTaskList().getBoard().getId(), // ID bảng
            card.getId(), // ID thẻ
            card.getTaskList().getBoard().getName() // Tên bảng
        );
        
        CardResponse response = toCardResponse(card); // Chuyển đổi
        webSocketService.broadcastToBoard(card.getTaskList().getBoard().getId(), "CARD_UPDATED", response, currentUser.getId()); // Broadcast
        
        log.info("Member {} assigned to card {}", userId, cardId); // Ghi log
    }

    @Override
    @Transactional
    public void unassignMember(Long cardId, Long userId, UserPrincipal currentUser) {
        Card card = findCardOrThrow(cardId); // Tìm card
        validateWriteAccess(card, currentUser.getId()); // Kiểm tra quyền

        User user = userRepository.findById(userId) // Tìm user
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        card.getMembers().remove(user); // Xóa thành viên
        cardRepository.save(card); // Lưu card
        
        CardResponse response = toCardResponse(card); // Chuyển đổi
        webSocketService.broadcastToBoard(card.getTaskList().getBoard().getId(), "CARD_UPDATED", response, currentUser.getId()); // Broadcast
        
        log.info("User {} unassigned from card {}", userId, cardId); // Ghi log
    }

    @Override
    @Transactional
    public CardResponse archiveCard(Long cardId, UserPrincipal currentUser) {
        Card card = findCardOrThrow(cardId); // Tìm card
        validateCardManagementAccess(card, currentUser.getId()); // Kiểm tra quyền quản lý
        User actor = findUserOrThrow(currentUser.getId()); // Tìm người thực hiện

        card.setIsArchived(true); // Đánh dấu archive
        card = cardRepository.save(card); // Lưu card

        activityService.logActivity(card, actor, "ARCHIVE_CARD", null); // Ghi log hoạt động
        log.info("Card archived: {}", cardId); // Ghi log

        CardResponse response = toCardResponse(card); // Chuyển đổi
        webSocketService.broadcastToBoard(card.getTaskList().getBoard().getId(), "CARD_ARCHIVED", cardId, currentUser.getId()); // Broadcast
        return response; // Trả về phản hồi
    }

    @Override
    @Transactional
    public CardResponse restoreCard(Long cardId, UserPrincipal currentUser) {
        Card card = findCardOrThrow(cardId); // Tìm card
        validateCardManagementAccess(card, currentUser.getId()); // Kiểm tra quyền
        User actor = findUserOrThrow(currentUser.getId()); // Tìm người thực hiện

        // Reset position to end of list
        Card lastCard = cardRepository.findTopByTaskListIdAndIsArchivedFalseOrderByPositionDesc(card.getTaskList().getId()); // Tìm card cuối
        int nextPosition = lastCard == null ? 0 : lastCard.getPosition() + 1; // Tính vị trí tiếp theo

        card.setIsArchived(false); // Bỏ archive
        card.setPosition(nextPosition); // Đặt vị trí mới
        card = cardRepository.save(card); // Lưu card

        activityService.logActivity(card, actor, "RESTORE_CARD", null); // Ghi log
        log.info("Card restored: {}", cardId); // Ghi log

        CardResponse response = toCardResponse(card); // Chuyển đổi
        webSocketService.broadcastToBoard(card.getTaskList().getBoard().getId(), "CARD_RESTORED", response, currentUser.getId());
        return response;
    }

    @Override
    public Page<CardResponse> getArchivedCards(Long boardId, int page, int size, UserPrincipal currentUser) {
        Board board = boardRepository.findById(boardId) // Tìm bảng
                .orElseThrow(() -> new ResourceNotFoundException("Board", boardId));
        validateMembership(board.getWorkspace().getId(), currentUser.getId()); // Kiểm tra quyền

        PageRequest pageRequest = PageRequest.of(page, size); // Tạo page request
        return cardRepository.findByTaskListBoardIdAndIsArchivedTrueOrderByUpdatedAtDesc(boardId, pageRequest) // Tìm card đã archive
                .map(this::toCardResponse); // Chuyển đổi
    }

    // ─── Dependency Graph ─────────────────────────────────

    @Override
    @Transactional
    public void addDependencies(Long cardId, CardDependencyRequest request, UserPrincipal currentUser) {
        Card card = findCardOrThrow(cardId); // Tìm card
        validateWriteAccess(card, currentUser.getId()); // Kiểm tra quyền

        List<Long> newParentIds = request.parentCardIds(); // Lấy danh sách ID cha mới
        for (Long parentId : newParentIds) { // Duyệt từng ID
            if (parentId.equals(cardId)) { // Nếu tự phụ thuộc
                throw new IllegalArgumentException("Card cannot depend on itself"); // Ném lỗi
            }
            Card parentCard = cardRepository.findById(parentId) // Tìm card cha
                    .orElseThrow(() -> new ResourceNotFoundException("Card", parentId));
            validateWriteAccess(parentCard, currentUser.getId()); // Kiểm tra quyền
        }

        List<Long> existingIds = parseParentIds(card.getParentIds()); // Lấy ID cha hiện tại
        Set<Long> merged = new HashSet<>(existingIds); // Hợp nhất
        merged.addAll(newParentIds); // Thêm ID mới

        List<Long> allParents = new ArrayList<>(merged); // Chuyển thành danh sách
        if (hasCycle(cardId, allParents)) { // Kiểm tra vòng lặp
            throw new IllegalStateException("Phát hiện vòng lặp dependency"); // Ném lỗi nếu có
        }

        try {
            card.setParentIds(objectMapper.writeValueAsString(allParents)); // Serialize thành JSON
        } catch (Exception e) {
            log.error("Failed to serialize parent IDs", e); // Ghi log lỗi
            throw new RuntimeException("Failed to save dependencies"); // Ném lỗi
        }
        cardRepository.save(card); // Lưu card
        log.info("Dependencies added to card {}: {}", cardId, allParents); // Ghi log
    }

    @Override
    @Transactional
    public void removeDependency(Long cardId, Long parentCardId, UserPrincipal currentUser) {
        Card card = findCardOrThrow(cardId); // Tìm card
        validateWriteAccess(card, currentUser.getId()); // Kiểm tra quyền

        List<Long> parentIds = parseParentIds(card.getParentIds()); // Lấy danh sách ID cha
        parentIds.remove(parentCardId); // Xóa ID cha khỏi danh sách

        try {
            card.setParentIds(parentIds.isEmpty() ? null : objectMapper.writeValueAsString(parentIds)); // Serialize hoặc null
        } catch (Exception e) {
            log.error("Failed to serialize parent IDs", e); // Ghi log lỗi
            throw new RuntimeException("Failed to remove dependency"); // Ném lỗi
        }
        cardRepository.save(card); // Lưu card
        log.info("Dependency removed from card {}: {}", cardId, parentCardId); // Ghi log
    }

    @Override
    public DependencyGraphResponse getDependencyGraph(Long cardId, UserPrincipal currentUser) {
        Card card = findCardOrThrow(cardId); // Tìm card
        validateMembership(card, currentUser.getId()); // Kiểm tra quyền

        CardInfoResponse cardInfo = toCardInfoResponse(card); // Chuyển đổi thông tin card

        List<Long> parentIds = parseParentIds(card.getParentIds()); // Lấy ID cha
        List<CardInfoResponse> blockedBy = parentIds.stream() // Lấy danh sách card chặn
                .map(id -> cardRepository.findById(id).orElse(null)) // Tìm card
                .filter(c -> c != null) // Lọc null
                .map(this::toCardInfoResponse) // Chuyển đổi
                .collect(Collectors.toList()); // Thu thập

        List<CardInfoResponse> blocking = cardRepository.findDependentCards(cardId).stream() // Lấy card phụ thuộc
                .map(this::toCardInfoResponse) // Chuyển đổi
                .collect(Collectors.toList()); // Thu thập

        return DependencyGraphResponse.builder()
                .card(cardInfo) // Gán thông tin card
                .blockedBy(blockedBy) // Gán danh sách card chặn
                .blocking(blocking) // Gán danh sách card bị chặn
                .build(); // Xây dựng DependencyGraphResponse
    }

    private List<Long> parseParentIds(String parentIdsJson) {
        if (parentIdsJson == null || parentIdsJson.isBlank()) { // Nếu null hoặc rỗng
            return new ArrayList<>(); // Trả về danh sách rỗng
        }
        try {
            return objectMapper.readValue(parentIdsJson, new TypeReference<List<Long>>() {}); // Parse JSON
        } catch (Exception e) {
            log.error("Failed to parse parent IDs JSON: {}", parentIdsJson, e); // Ghi log lỗi
            return new ArrayList<>(); // Trả về danh sách rỗng
        }
    }

    private boolean hasCycle(Long cardId, List<Long> parentIds) {
        for (Long parentId : parentIds) { // Duyệt từng ID cha
            if (parentId.equals(cardId)) return true; // Phát hiện vòng lặp
            Card parent = cardRepository.findById(parentId).orElse(null); // Tìm card cha
            if (parent == null) continue; // Bỏ qua nếu không tìm thấy
            List<Long> grandParentIds = parseParentIds(parent.getParentIds()); // Lấy ID cha của cha
            if (hasCycle(cardId, grandParentIds)) return true; // Kiểm tra đệ quy
        }
        return false; // Không có vòng lặp
    }

    private CardInfoResponse toCardInfoResponse(Card card) {
        return CardInfoResponse.builder()
                .id(card.getId()) // Gán ID
                .listId(card.getTaskList().getId()) // Gán ID list
                .title(card.getTitle()) // Gán tiêu đề
                .priority(card.getPriority().name()) // Gán độ ưu tiên
                .isArchived(card.getIsArchived()) // Gán trạng thái archive
                .dueDate(card.getDueDate()) // Gán hạn chót
                .listName(card.getTaskList().getName()) // Gán tên list
                .boardName(card.getTaskList().getBoard().getName()) // Gán tên board
                .build(); // Xây dựng CardInfoResponse
    }

    // ─── Helpers ────────────────────────────────────────

    private Card findCardOrThrow(Long id) {
        return cardRepository.findById(id) // Tìm card theo ID
                .orElseThrow(() -> new ResourceNotFoundException("Card", id)); // Ném lỗi nếu không tìm thấy
    }

    private TaskList findListOrThrow(Long id) {
        return taskListRepository.findById(id) // Tìm list theo ID
                .orElseThrow(() -> new ResourceNotFoundException("List", id)); // Ném lỗi nếu không tìm thấy
    }

    private void validateMembership(Card card, Long userId) {
        validateMembership(card.getTaskList().getBoard().getWorkspace().getId(), userId); // Kiểm tra thành viên workspace
    }

    private void validateMembership(Long workspaceId, Long userId) {
        if (!memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) { // Nếu không phải thành viên
            throw new UnauthorizedException("You are not a member of this workspace"); // Ném lỗi
        }
    }

    private void validateWriteAccess(Card card, Long userId) {
        validateWriteAccess(card.getTaskList().getBoard().getWorkspace().getId(), userId); // Kiểm tra quyền ghi
    }

    private void validateWriteAccess(TaskList taskList, Long userId) {
        validateWriteAccess(taskList.getBoard().getWorkspace().getId(), userId); // Kiểm tra quyền ghi
    }

    private void validateWriteAccess(Long workspaceId, Long userId) {
        boolean hasAccess = memberRepository.existsByWorkspaceIdAndUserIdAndRoleIn( // Kiểm tra quyền
                workspaceId, userId, List.of(com.okabe.entity.enums.Role.OWNER, com.okabe.entity.enums.Role.ADMIN, com.okabe.entity.enums.Role.MEMBER));
        if (!hasAccess) { // Nếu không có quyền
            throw new UnauthorizedException("You do not have permission to perform this action"); // Ném lỗi
        }
    }

    private void validateCardManagementAccess(Card card, Long userId) {
        Long workspaceId = card.getTaskList().getBoard().getWorkspace().getId(); // Lấy workspace ID
        com.okabe.entity.WorkspaceMember member = memberRepository.findByWorkspaceIdAndUserId(workspaceId, userId) // Tìm thành viên
                .orElseThrow(() -> new UnauthorizedException("You are not a member of this workspace")); // Ném lỗi nếu không tìm thấy
        
        boolean isOwnerOrAdmin = member.getRole() == com.okabe.entity.enums.Role.OWNER || member.getRole() == com.okabe.entity.enums.Role.ADMIN; // Kiểm tra role
        boolean isCreator = card.getCreatedBy().getId().equals(userId); // Kiểm tra người tạo

        if (!isOwnerOrAdmin && !isCreator) { // Nếu không có quyền
            throw new UnauthorizedException("You can only archive/restore your own cards"); // Ném lỗi
        }
    }

    private CardResponse toCardResponse(Card card) {
        List<LabelResponse> labelResponses = card.getLabels().stream() // Chuyển đổi labels
                .map(l -> LabelResponse.builder()
                        .id(l.getId()) // Gán ID
                        .boardId(l.getBoard().getId()) // Gán board ID
                        .name(l.getName()) // Gán tên
                        .color(l.getColor()) // Gán màu
                        .build())
                .collect(Collectors.toList());

        List<ChecklistResponse> checklistResponses = card.getChecklists().stream() // Chuyển đổi checklists
                .map(c -> ChecklistResponse.builder()
                        .id(c.getId()) // Gán ID
                        .cardId(card.getId()) // Gán card ID
                        .name(c.getName()) // Gán tên
                        .position(c.getPosition()) // Gán vị trí
                        .items(c.getItems().stream() // Chuyển đổi items
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

        List<UserResponse> memberResponses = card.getMembers().stream() // Chuyển đổi members
                .map(m -> UserResponse.builder()
                        .id(m.getId()) // Gán ID
                        .username(m.getUsername()) // Gán tên
                        .email(m.getEmail()) // Gán email
                        .avatarUrl(m.getAvatarUrl()) // Gán avatar
                        .build())
                .collect(Collectors.toList());

        List<AttachmentResponse> attachmentResponses = card.getAttachments().stream() // Chuyển đổi attachments
                .map(a -> AttachmentResponse.builder()
                        .id(a.getId()) // Gán ID
                        .cardId(card.getId()) // Gán card ID
                        .uploadedById(a.getUploadedBy().getId()) // Gán người tải
                        .uploadedByUsername(a.getUploadedBy().getUsername()) // Gán tên người tải
                        .filename(a.getFilename()) // Gán tên file
                        .url(a.getStorageKey()) // Gán URL
                        .fileSize(a.getFileSize()) // Gán kích thước
                        .mimeType(a.getMimeType()) // Gán loại MIME
                        .createdAt(a.getCreatedAt()) // Gán thời gian
                        .build())
                .collect(Collectors.toList());

        return CardResponse.builder()
                .id(card.getId()) // Gán ID
                .listId(card.getTaskList().getId()) // Gán list ID
                .title(card.getTitle()) // Gán tiêu đề
                .description(card.getDescription()) // Gán mô tả
                .position(card.getPosition()) // Gán vị trí
                .dueDate(card.getDueDate()) // Gán hạn chót
                .startDate(card.getStartDate()) // Gán ngày bắt đầu
                .priority(card.getPriority().name()) // Gán độ ưu tiên
                .isArchived(card.getIsArchived()) // Gán trạng thái archive
                .totalFocusMinutes(card.getTotalFocusMinutes()) // Gán tổng phút focus
                .createdById(card.getCreatedBy().getId()) // Gán ID người tạo
                .createdByName(card.getCreatedBy().getUsername()) // Gán tên người tạo
                .createdAt(card.getCreatedAt()) // Gán thời gian tạo
                .labels(labelResponses) // Gán labels
                .checklists(checklistResponses) // Gán checklists
                .members(memberResponses) // Gán members
                .attachments(attachmentResponses) // Gán attachments
                .build(); // Xây dựng CardResponse
    }

    @Override
    public List<CardSelectionResponse> getWorkspaceCards(Long workspaceId, UserPrincipal currentUser) {
        validateMembership(workspaceId, currentUser.getId()); // Kiểm tra quyền
        List<Card> cards = cardRepository.findByWorkspaceId(workspaceId); // Lấy card theo workspace
        return cards.stream() // Xử lý từng card
                .map(c -> CardSelectionResponse.builder()
                        .id(c.getId()) // Gán ID
                        .title(c.getTitle()) // Gán tiêu đề
                        .boardId(c.getTaskList().getBoard().getId()) // Gán board ID
                        .boardName(c.getTaskList().getBoard().getName()) // Gán tên board
                        .listName(c.getTaskList().getName()) // Gán tên list
                        .build()) // Xây dựng CardSelectionResponse
                .collect(Collectors.toList()); // Thu thập thành danh sách
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id) // Tìm user theo ID
                .orElseThrow(() -> new ResourceNotFoundException("User", id)); // Ném lỗi nếu không tìm thấy
    }

    private boolean isCompletedList(String name) {
        String lower = name.toLowerCase(); // Chuyển thành chữ thường
        return lower.contains("done") || lower.contains("completed") || lower.contains("closed") || lower.contains("hoàn thành"); // Kiểm tra tên cột hoàn thành
    }
}
