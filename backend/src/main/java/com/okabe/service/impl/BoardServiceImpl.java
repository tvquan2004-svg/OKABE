package com.okabe.service.impl;

import com.okabe.dto.request.CreateBoardRequest;
import com.okabe.dto.request.ReorderBoardRequest;
import com.okabe.dto.request.UpdateBoardRequest;
import com.okabe.dto.response.*;
import com.okabe.entity.Board;
import com.okabe.entity.Card;
import com.okabe.entity.TaskList;
import com.okabe.entity.Workspace;
import com.okabe.entity.User;
import com.okabe.entity.enums.Role;
import com.okabe.exception.ResourceNotFoundException;
import com.okabe.exception.UnauthorizedException;
import com.okabe.repository.*;
import com.okabe.security.UserPrincipal;
import com.okabe.service.BoardService;
import com.okabe.service.EmailNotificationService;
import com.okabe.service.NotificationService;
import com.okabe.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardServiceImpl implements BoardService {

    private final BoardRepository boardRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final TaskListRepository taskListRepository;
    private final CardRepository cardRepository;
    private final StorageService storageService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EmailNotificationService emailNotificationService;
    private final BoardTemplateRepository boardTemplateRepository;

    @Override
    public List<BoardResponse> getBoardsByWorkspace(Long workspaceId, UserPrincipal currentUser) {
        validateWorkspaceMembership(workspaceId, currentUser.getId()); // Kiểm tra quyền thành viên
        List<Board> boards = boardRepository.findByWorkspaceIdAndIsArchivedFalseOrderByPositionAscCreatedAtAsc(workspaceId); // Lấy danh sách bảng
        return boards.stream().map(b -> toBoardResponse(b, false)).toList(); // Chuyển đổi và trả về
    }

    @Override
    public BoardResponse getBoard(Long boardId, UserPrincipal currentUser) {
        Board board = findBoardOrThrow(boardId); // Tìm bảng theo ID
        validateWorkspaceMembership(board.getWorkspace().getId(), currentUser.getId()); // Kiểm tra quyền
        return toBoardResponse(board, true); // Trả về phản hồi kèm danh sách
    }

    @Override
    @Transactional
    public BoardResponse createBoard(Long workspaceId, CreateBoardRequest request, UserPrincipal currentUser) {
        validateWorkspaceWriteAccess(workspaceId, currentUser.getId()); // Kiểm tra quyền ghi

        Workspace workspace = workspaceRepository.findById(workspaceId) // Tìm workspace
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", workspaceId));

        Board lastBoard = boardRepository.findTopByWorkspaceIdAndIsArchivedFalseOrderByPositionDesc(workspaceId); // Tìm bảng cuối
        int nextPosition = lastBoard == null ? 0 : lastBoard.getPosition() + 1; // Tính vị trí tiếp theo

        Board board = Board.builder()
                .workspace(workspace) // Gán workspace
                .name(request.name()) // Gán tên bảng
                .description(request.description()) // Gán mô tả
                .position(nextPosition) // Gán vị trí
                .background(request.background()) // Gán background
                .build(); // Xây dựng Board

        board = boardRepository.save(board); // Lưu bảng

        if (request.templateId() != null) { // Nếu có template
            com.okabe.entity.BoardTemplate template = boardTemplateRepository.findById(request.templateId()) // Tìm template
                    .orElseThrow(() -> new ResourceNotFoundException("BoardTemplate", request.templateId()));

            User creator = userRepository.findById(currentUser.getId()).orElseThrow(); // Lấy người tạo

            for (com.okabe.entity.TemplateList templateList : template.getLists()) { // Duyệt danh sách template
                TaskList taskList = TaskList.builder()
                        .board(board) // Gán board
                        .name(templateList.getName()) // Gán tên cột
                        .position(templateList.getPosition()) // Gán vị trí
                        .build(); // Xây dựng TaskList
                taskList = taskListRepository.save(taskList); // Lưu cột

                for (com.okabe.entity.TemplateCard templateCard : templateList.getCards()) { // Duyệt card template
                    Card card = Card.builder()
                            .taskList(taskList) // Gán cột
                            .title(templateCard.getTitle()) // Gán tiêu đề
                            .description(templateCard.getDescription()) // Gán mô tả
                            .position(templateCard.getPosition()) // Gán vị trí
                            .createdBy(creator) // Gán người tạo
                            .build(); // Xây dựng Card
                    cardRepository.save(card); // Lưu card
                }
            }
        }

        log.info("Board created: {} in workspace {}", board.getName(), workspaceId); // Ghi log
        return toBoardResponse(board, request.templateId() != null); // Trả về phản hồi
    }

    @Override
    @Transactional
    public BoardResponse updateBoard(Long boardId, UpdateBoardRequest request, UserPrincipal currentUser) {
        Board board = findBoardOrThrow(boardId); // Tìm bảng
        validateWorkspaceWriteAccess(board.getWorkspace().getId(), currentUser.getId()); // Kiểm tra quyền

        if (request.name() != null) board.setName(request.name()); // Cập nhật tên
        if (request.description() != null) board.setDescription(request.description()); // Cập nhật mô tả
        if (request.background() != null) board.setBackground(request.background()); // Cập nhật background
        if (request.isStarred() != null) board.setIsStarred(request.isStarred()); // Cập nhật trạng thái gắn sao
        if (request.isArchived() != null) board.setIsArchived(request.isArchived()); // Cập nhật trạng thái archive

        board = boardRepository.save(board); // Lưu thay đổi
        return toBoardResponse(board, false); // Trả về phản hồi
    }

    @Override
    @Transactional
    public void reorderBoards(Long workspaceId, ReorderBoardRequest request, UserPrincipal currentUser) {
        validateWorkspaceWriteAccess(workspaceId, currentUser.getId()); // Kiểm tra quyền

        List<Board> boards = boardRepository.findByWorkspaceIdAndIsArchivedFalseOrderByPositionAscCreatedAtAsc(workspaceId); // Lấy danh sách bảng
        Map<Long, Board> boardById = boards.stream() // Tạo map ID -> Board
                .collect(Collectors.toMap(Board::getId, Function.identity()));

        List<Long> orderedIds = request.orderedIds(); // Lấy danh sách ID đã sắp xếp
        if (boards.size() != orderedIds.size() || !boardById.keySet().containsAll(orderedIds)) { // Kiểm tra tính hợp lệ
            throw new ResourceNotFoundException("Board", workspaceId); // Ném lỗi nếu không khớp
        }

        for (int index = 0; index < orderedIds.size(); index++) { // Duyệt danh sách
            Board board = boardById.get(orderedIds.get(index)); // Lấy bảng
            board.setPosition(index); // Đặt vị trí mới
        }

        boardRepository.saveAll(boards); // Lưu tất cả thay đổi
        log.info("Boards reordered in workspace {}", workspaceId); // Ghi log
    }

    @Override
    @Transactional
    public void deleteBoard(Long boardId, UserPrincipal currentUser) {
        Board board = findBoardOrThrow(boardId); // Tìm bảng
        validateWorkspaceAdmin(board.getWorkspace().getId(), currentUser.getId()); // Kiểm tra quyền admin
        
        // Delete background image if it exists
        if (board.getBackground() != null && board.getBackground().startsWith("http")) { // Nếu có background URL
            storageService.delete(board.getBackground()); // Xóa ảnh nền
        }
        
        boardRepository.delete(board); // Xóa bảng
        log.info("Board deleted: {}", boardId); // Ghi log
    }

    @Override
    @Transactional
    public BoardResponse updateBackground(Long boardId, String type, String colorValue, org.springframework.web.multipart.MultipartFile file, UserPrincipal currentUser) {
        Board board = findBoardOrThrow(boardId); // Tìm bảng
        validateWorkspaceAdmin(board.getWorkspace().getId(), currentUser.getId()); // Kiểm tra quyền admin

        String backgroundValue = board.getBackground(); // Lấy giá trị background hiện tại

        if ("COLOR".equalsIgnoreCase(type)) { // Nếu là màu hex
            if (colorValue == null || !colorValue.matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$")) { // Kiểm tra định dạng
                throw new IllegalArgumentException("Invalid hex color format"); // Lỗi nếu không hợp lệ
            }
            backgroundValue = colorValue; // Gán màu hex
        } else if ("PRESET".equalsIgnoreCase(type)) { // Nếu là ảnh preset
            // Accept preset local image paths like /backgrounds/mau-background-dep-1.jpg
            if (colorValue == null || colorValue.isBlank()) { // Kiểm tra rỗng
                throw new IllegalArgumentException("Preset image path cannot be empty");
            }
            backgroundValue = colorValue; // Gán đường dẫn preset
        } else if ("IMAGE".equalsIgnoreCase(type) && file != null && !file.isEmpty()) { // Nếu upload ảnh
            try {
                // Delete old image if it was a remote image
                if (board.getBackground() != null && board.getBackground().startsWith("http")) { // Nếu có ảnh cũ
                    storageService.delete(board.getBackground()); // Xóa ảnh cũ
                }
                backgroundValue = storageService.upload(file); // Upload ảnh mới
                log.info("Board background updated: {}", backgroundValue); // Ghi log
            } catch (Exception e) {
                log.error("Failed to upload board background", e); // Ghi lỗi
                throw new RuntimeException("Failed to upload image: " + e.getMessage()); // Ném lỗi
            }
        }

        board.setBackground(backgroundValue); // Cập nhật background
        board = boardRepository.save(board); // Lưu bảng
        log.info("Board {} background updated to {}", boardId, backgroundValue); // Ghi log
        return toBoardResponse(board, false); // Trả về phản hồi
    }

    @Override
    @Transactional
    public void inviteMember(Long boardId, String email, UserPrincipal currentUser) {
        Board board = findBoardOrThrow(boardId); // Tìm bảng
        validateWorkspaceWriteAccess(board.getWorkspace().getId(), currentUser.getId()); // Kiểm tra quyền

        User recipient = userRepository.findByEmail(email) // Tìm người nhận
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        User actor = userRepository.findById(currentUser.getId()).orElseThrow(); // Lấy người mời

        if (!memberRepository.existsByWorkspaceIdAndUserId(board.getWorkspace().getId(), recipient.getId())) { // Nếu chưa có trong workspace
             com.okabe.entity.WorkspaceMember newMember = com.okabe.entity.WorkspaceMember.builder() // Tạo thành viên mới
                     .workspaceId(board.getWorkspace().getId())
                     .userId(recipient.getId())
                     .role(com.okabe.entity.enums.Role.MEMBER) // Vai trò MEMBER
                     .build();
             memberRepository.save(newMember); // Lưu thành viên
             log.info("User {} added to workspace {} because of board invitation", email, board.getWorkspace().getId());
        }

        notificationService.createNotification( // Tạo thông báo mời
                recipient,
                actor,
                "BOARD_INVITATION",
                "BOARD",
                boardId,
                boardId,
                String.format("%s đã mời bạn cùng cộng tác tại bảng: %s", actor.getUsername(), board.getName())
        );

        emailNotificationService.sendBoardInvitationEmail(actor, recipient, board.getName(), boardId); // Gửi email mời
        log.info("Board invitation sent to {} for board {}", email, boardId); // Ghi log
    }

    @Override
    @Transactional
    public BoardResponse archiveBoard(Long boardId, UserPrincipal currentUser) {
        Board board = findBoardOrThrow(boardId); // Tìm bảng
        validateWorkspaceAdmin(board.getWorkspace().getId(), currentUser.getId()); // Kiểm tra quyền admin
        board.setIsArchived(true); // Đánh dấu archive
        board = boardRepository.save(board); // Lưu thay đổi
        log.info("Board archived: {}", boardId); // Ghi log
        return toBoardResponse(board, false); // Trả về phản hồi
    }

    @Override
    @Transactional
    public BoardResponse restoreBoard(Long boardId, UserPrincipal currentUser) {
        Board board = findBoardOrThrow(boardId); // Tìm bảng
        validateWorkspaceAdmin(board.getWorkspace().getId(), currentUser.getId()); // Kiểm tra quyền admin

        // Reset position to end
        Board lastBoard = boardRepository.findTopByWorkspaceIdAndIsArchivedFalseOrderByPositionDesc(board.getWorkspace().getId()); // Tìm bảng cuối
        int nextPosition = lastBoard == null ? 0 : lastBoard.getPosition() + 1; // Tính vị trí mới

        board.setIsArchived(false); // Bỏ archive
        board.setPosition(nextPosition); // Đặt vị trí
        board = boardRepository.save(board); // Lưu thay đổi
        log.info("Board restored: {}", boardId); // Ghi log
        return toBoardResponse(board, false); // Trả về phản hồi
    }

    @Override
    public List<BoardResponse> getArchivedBoards(Long workspaceId, UserPrincipal currentUser) {
        validateWorkspaceMembership(workspaceId, currentUser.getId()); // Kiểm tra quyền
        List<Board> boards = boardRepository.findByWorkspaceIdAndIsArchivedTrueOrderByPositionAscCreatedAtAsc(workspaceId); // Lấy danh sách archive
        return boards.stream().map(b -> toBoardResponse(b, false)).toList(); // Chuyển đổi và trả về
    }

    @Override
    @Transactional
    public BoardResponse updateVisibility(Long boardId, boolean isPublic, UserPrincipal currentUser) {
        Board board = findBoardOrThrow(boardId); // Tìm bảng
        validateWorkspaceAdmin(board.getWorkspace().getId(), currentUser.getId()); // Kiểm tra quyền admin

        board.setIsPublic(isPublic); // Cập nhật công khai
        if (isPublic && board.getPublicToken() == null) { // Nếu công khai và chưa có token
            board.setPublicToken(UUID.randomUUID().toString().replace("-", "")); // Tạo token mới
        } else if (!isPublic) { // Nếu không công khai
            board.setPublicToken(null); // Xóa token
        }

        board = boardRepository.save(board); // Lưu thay đổi
        log.info("Board {} visibility updated to public: {}", boardId, isPublic); // Ghi log
        return toBoardResponse(board, false); // Trả về phản hồi
    }

    @Override
    @Transactional(readOnly = true)
    public BoardPublicDto getPublicBoard(String token) {
        Board board = boardRepository.findByPublicToken(token) // Tìm bảng theo token
                .orElseThrow(() -> new ResourceNotFoundException("Public Board", token));
        
        if (!board.getIsPublic()) { // Nếu bảng không còn công khai
            throw new UnauthorizedException("This board is no longer public"); // Ném lỗi
        }

        return toBoardPublicDto(board); // Trả về DTO công khai
    }

    // ─── Helpers ────────────────────────────────────────

    private Board findBoardOrThrow(Long id) {
        return boardRepository.findById(id) // Tìm bảng theo ID
                .orElseThrow(() -> new ResourceNotFoundException("Board", id)); // Ném lỗi nếu không tìm thấy
    }

    private void validateWorkspaceMembership(Long workspaceId, Long userId) {
        if (!memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) { // Nếu không phải thành viên
            throw new UnauthorizedException("You are not a member of this workspace"); // Ném lỗi
        }
    }

    private void validateWorkspaceWriteAccess(Long workspaceId, Long userId) {
        boolean hasAccess = memberRepository.existsByWorkspaceIdAndUserIdAndRoleIn( // Kiểm tra quyền ghi
                workspaceId, userId, List.of(Role.OWNER, Role.ADMIN, Role.MEMBER));
        if (!hasAccess) { // Nếu không có quyền
            throw new UnauthorizedException("You do not have permission to perform this action"); // Ném lỗi
        }
    }

    private void validateWorkspaceAdmin(Long workspaceId, Long userId) {
        boolean hasAccess = memberRepository.existsByWorkspaceIdAndUserIdAndRoleIn( // Kiểm tra quyền admin
                workspaceId, userId, List.of(Role.OWNER, Role.ADMIN));
        if (!hasAccess) { // Nếu không có quyền
            throw new UnauthorizedException("Only OWNER or ADMIN can perform this action"); // Ném lỗi
        }
    }

    private BoardResponse toBoardResponse(Board board, boolean includeLists) {
        List<TaskList> taskLists = taskListRepository // Lấy danh sách cột
                .findByBoardIdAndIsArchivedFalseOrderByPositionAsc(board.getId());

        int listCount = taskLists.size(); // Đếm số cột
        int totalCards = 0; // Khởi tạo tổng số card

        List<ListResponse> lists = null;
        if (includeLists) { // Nếu cần bao gồm danh sách
            lists = taskLists.stream().map(this::toListResponse).toList(); // Chuyển đổi cột
            totalCards = lists.stream() // Tính tổng card từ danh sách
                    .mapToInt(l -> l.getCards() != null ? l.getCards().size() : 0)
                    .sum();
        } else { // Nếu không cần danh sách
            for (TaskList tl : taskLists) { // Duyệt cột
                totalCards += cardRepository.countByTaskListIdAndIsArchivedFalse(tl.getId()); // Đếm card mỗi cột
            }
        }

        return BoardResponse.builder() // Xây dựng phản hồi
                .id(board.getId())
                .workspaceId(board.getWorkspace().getId())
                .name(board.getName())
                .description(board.getDescription())
                .position(board.getPosition())
                .background(board.getBackground())
                .isStarred(board.getIsStarred())
                .isArchived(board.getIsArchived())
                .isPublic(board.getIsPublic())
                .publicToken(board.getPublicToken())
                .listCount(listCount)
                .totalCards(totalCards)
                .createdAt(board.getCreatedAt())
                .lists(lists)
                .build();
    }

    private BoardPublicDto toBoardPublicDto(Board board) {
        List<TaskList> taskLists = taskListRepository // Lấy danh sách cột
                .findByBoardIdAndIsArchivedFalseOrderByPositionAsc(board.getId());

        List<ListResponse> lists = taskLists.stream().map(this::toPublicListResponse).toList(); // Chuyển đổi cột công khai

        List<BoardPublicDto.PublicUserResponse> publicMembers = board.getWorkspace() != null // Nếu có workspace
            ? memberRepository.findByWorkspaceId(board.getWorkspace().getId()).stream() // Lấy danh sách thành viên
                .map(m -> BoardPublicDto.PublicUserResponse.builder() // Xây dựng phản hồi công khai
                        .id(m.getUser().getId())
                        .username(m.getUser().getUsername())
                        .avatarUrl(m.getUser().getAvatarUrl())
                        .build())
                .toList()
            : List.of(); // Trả về danh sách rỗng nếu không có

        return BoardPublicDto.builder() // Xây dựng DTO
                .id(board.getId())
                .name(board.getName())
                .description(board.getDescription())
                .background(board.getBackground())
                .createdAt(board.getCreatedAt())
                .lists(lists)
                .members(publicMembers)
                .build();
    }

    private ListResponse toPublicListResponse(TaskList taskList) {
        List<Card> cards = cardRepository // Lấy danh sách card
                .findByTaskListIdAndIsArchivedFalseOrderByPositionAsc(taskList.getId());

        return ListResponse.builder() // Xây dựng phản hồi cột công khai
                .id(taskList.getId())
                .boardId(taskList.getBoard().getId())
                .name(taskList.getName())
                .position(taskList.getPosition())
                .cards(cards.stream().map(this::toPublicCardResponse).toList())
                .build();
    }

    private CardResponse toPublicCardResponse(Card card) {
        CardResponse response = toCardResponse(card); // Chuyển đổi card
        // Clear emails from members for public view
        if (response.getMembers() != null) { // Nếu có thành viên
            response.setMembers(response.getMembers().stream() // Ẩn email thành viên
                .map(m -> UserResponse.builder()
                    .id(m.getId())
                    .username(m.getUsername())
                    .avatarUrl(m.getAvatarUrl())
                    .email(null) // Hide email
                    .build())
                .toList());
        }
        return response; // Trả về phản hồi đã ẩn email
    }

    private ListResponse toListResponse(TaskList taskList) {
        List<Card> cards = cardRepository // Lấy danh sách card
                .findByTaskListIdAndIsArchivedFalseOrderByPositionAsc(taskList.getId());

        return ListResponse.builder() // Xây dựng phản hồi cột
                .id(taskList.getId())
                .boardId(taskList.getBoard().getId())
                .name(taskList.getName())
                .position(taskList.getPosition())
                .cards(cards.stream().map(this::toCardResponse).toList())
                .build();
    }

    private CardResponse toCardResponse(Card card) {
        List<LabelResponse> labelResponses = card.getLabels().stream() // Chuyển đổi nhãn
                .map(l -> LabelResponse.builder()
                        .id(l.getId())
                        .boardId(l.getBoard().getId())
                        .name(l.getName())
                        .color(l.getColor())
                        .build())
                .collect(Collectors.toList());

        List<ChecklistResponse> checklistResponses = card.getChecklists().stream() // Chuyển đổi checklist
                .map(c -> ChecklistResponse.builder()
                        .id(c.getId())
                        .cardId(card.getId())
                        .name(c.getName())
                        .position(c.getPosition())
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

        return CardResponse.builder() // Xây dựng phản hồi card
                .id(card.getId())
                .listId(card.getTaskList().getId())
                .title(card.getTitle())
                .description(card.getDescription())
                .position(card.getPosition())
                .dueDate(card.getDueDate())
                .startDate(card.getStartDate())
                .priority(card.getPriority() != null ? card.getPriority().name() : "MEDIUM")
                .isArchived(card.getIsArchived())
                .totalFocusMinutes(card.getTotalFocusMinutes())
                .createdById(card.getCreatedBy() != null ? card.getCreatedBy().getId() : null)
                .createdByName(card.getCreatedBy() != null ? card.getCreatedBy().getUsername() : "Hệ thống")
                .createdAt(card.getCreatedAt())
                .labels(labelResponses)
                .checklists(checklistResponses)
                .members(card.getMembers().stream() // Chuyển đổi thành viên
                        .map(m -> UserResponse.builder()
                                .id(m.getId())
                                .username(m.getUsername())
                                .email(m.getEmail())
                                .avatarUrl(m.getAvatarUrl())
                                .build())
                        .collect(Collectors.toList()))
                .attachments(card.getAttachments().stream() // Chuyển đổi tệp đính kèm
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
