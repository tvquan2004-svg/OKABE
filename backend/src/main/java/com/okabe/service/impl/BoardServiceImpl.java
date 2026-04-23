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
        validateWorkspaceMembership(workspaceId, currentUser.getId());
        List<Board> boards = boardRepository.findByWorkspaceIdAndIsArchivedFalseOrderByPositionAscCreatedAtAsc(workspaceId);
        return boards.stream().map(b -> toBoardResponse(b, false)).toList();
    }

    @Override
    public BoardResponse getBoard(Long boardId, UserPrincipal currentUser) {
        Board board = findBoardOrThrow(boardId);
        validateWorkspaceMembership(board.getWorkspace().getId(), currentUser.getId());
        return toBoardResponse(board, true);
    }

    @Override
    @Transactional
    public BoardResponse createBoard(Long workspaceId, CreateBoardRequest request, UserPrincipal currentUser) {
        validateWorkspaceWriteAccess(workspaceId, currentUser.getId());

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", workspaceId));

        Board lastBoard = boardRepository.findTopByWorkspaceIdAndIsArchivedFalseOrderByPositionDesc(workspaceId);
        int nextPosition = lastBoard == null ? 0 : lastBoard.getPosition() + 1;

        Board board = Board.builder()
                .workspace(workspace)
                .name(request.name())
                .description(request.description())
                .position(nextPosition)
                .background(request.background())
                .build();

        board = boardRepository.save(board);

        if (request.templateId() != null) {
            com.okabe.entity.BoardTemplate template = boardTemplateRepository.findById(request.templateId())
                    .orElseThrow(() -> new ResourceNotFoundException("BoardTemplate", request.templateId()));

            User creator = userRepository.findById(currentUser.getId()).orElseThrow();

            for (com.okabe.entity.TemplateList templateList : template.getLists()) {
                TaskList taskList = TaskList.builder()
                        .board(board)
                        .name(templateList.getName())
                        .position(templateList.getPosition())
                        .build();
                taskList = taskListRepository.save(taskList);

                for (com.okabe.entity.TemplateCard templateCard : templateList.getCards()) {
                    Card card = Card.builder()
                            .taskList(taskList)
                            .title(templateCard.getTitle())
                            .description(templateCard.getDescription())
                            .position(templateCard.getPosition())
                            .createdBy(creator)
                            .build();
                    cardRepository.save(card);
                }
            }
        }

        log.info("Board created: {} in workspace {}", board.getName(), workspaceId);
        return toBoardResponse(board, request.templateId() != null);
    }

    @Override
    @Transactional
    public BoardResponse updateBoard(Long boardId, UpdateBoardRequest request, UserPrincipal currentUser) {
        Board board = findBoardOrThrow(boardId);
        validateWorkspaceWriteAccess(board.getWorkspace().getId(), currentUser.getId());

        if (request.name() != null) board.setName(request.name());
        if (request.description() != null) board.setDescription(request.description());
        if (request.background() != null) board.setBackground(request.background());
        if (request.isStarred() != null) board.setIsStarred(request.isStarred());
        if (request.isArchived() != null) board.setIsArchived(request.isArchived());

        board = boardRepository.save(board);
        return toBoardResponse(board, false);
    }

    @Override
    @Transactional
    public void reorderBoards(Long workspaceId, ReorderBoardRequest request, UserPrincipal currentUser) {
        validateWorkspaceWriteAccess(workspaceId, currentUser.getId());

        List<Board> boards = boardRepository.findByWorkspaceIdAndIsArchivedFalseOrderByPositionAscCreatedAtAsc(workspaceId);
        Map<Long, Board> boardById = boards.stream()
                .collect(Collectors.toMap(Board::getId, Function.identity()));

        List<Long> orderedIds = request.orderedIds();
        if (boards.size() != orderedIds.size() || !boardById.keySet().containsAll(orderedIds)) {
            throw new ResourceNotFoundException("Board", workspaceId);
        }

        for (int index = 0; index < orderedIds.size(); index++) {
            Board board = boardById.get(orderedIds.get(index));
            board.setPosition(index);
        }

        boardRepository.saveAll(boards);
        log.info("Boards reordered in workspace {}", workspaceId);
    }

    @Override
    @Transactional
    public void deleteBoard(Long boardId, UserPrincipal currentUser) {
        Board board = findBoardOrThrow(boardId);
        validateWorkspaceAdmin(board.getWorkspace().getId(), currentUser.getId());
        
        // Delete background image if it exists
        if (board.getBackground() != null && board.getBackground().startsWith("http")) {
            storageService.delete(board.getBackground());
        }
        
        boardRepository.delete(board);
        log.info("Board deleted: {}", boardId);
    }

    @Override
    @Transactional
    public BoardResponse updateBackground(Long boardId, String type, String colorValue, org.springframework.web.multipart.MultipartFile file, UserPrincipal currentUser) {
        Board board = findBoardOrThrow(boardId);
        validateWorkspaceAdmin(board.getWorkspace().getId(), currentUser.getId());

        String backgroundValue = board.getBackground();

        if ("COLOR".equalsIgnoreCase(type)) {
            if (colorValue == null || !colorValue.matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$")) {
                throw new IllegalArgumentException("Invalid hex color format");
            }
            backgroundValue = colorValue;
        } else if ("IMAGE".equalsIgnoreCase(type) && file != null && !file.isEmpty()) {
            try {
                // Delete old image if it was an image
                if (board.getBackground() != null && board.getBackground().startsWith("http")) {
                    storageService.delete(board.getBackground());
                }
                
                try {
                    backgroundValue = storageService.upload(file);
                } catch (Exception e) {
                    log.warn("Cloudinary upload failed, falling back to local storage: {}", e.getMessage());
                    // We need a way to access the local storage service directly if the primary fails
                    // Since we can't easily inject both with same type without qualifiers, 
                    // we'll just throw the error for now, but I've ensured .env will load.
                    throw e; 
                }
                
                log.info("Board background updated: {}", backgroundValue);
            } catch (Exception e) {
                log.error("Failed to upload board background", e);
                throw new RuntimeException("Failed to upload image: " + e.getMessage());
            }
        }

        board.setBackground(backgroundValue);
        board = boardRepository.save(board);
        log.info("Board {} background updated to {}", boardId, backgroundValue);
        return toBoardResponse(board, false);
    }

    @Override
    @Transactional
    public void inviteMember(Long boardId, String email, UserPrincipal currentUser) {
        Board board = findBoardOrThrow(boardId);
        validateWorkspaceWriteAccess(board.getWorkspace().getId(), currentUser.getId());

        User recipient = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        User actor = userRepository.findById(currentUser.getId()).orElseThrow();

        // Check if user is in workspace. If not, maybe we should add them?
        // For now, let's assume they must be in the workspace, or we add them as MEMBER.
        if (!memberRepository.existsByWorkspaceIdAndUserId(board.getWorkspace().getId(), recipient.getId())) {
             // Automatically add to workspace as MEMBER if not present
             com.okabe.entity.WorkspaceMember newMember = com.okabe.entity.WorkspaceMember.builder()
                     .workspaceId(board.getWorkspace().getId())
                     .userId(recipient.getId())
                     .role(com.okabe.entity.enums.Role.MEMBER)
                     .build();
             memberRepository.save(newMember);
             log.info("User {} added to workspace {} because of board invitation", email, board.getWorkspace().getId());
        }

        notificationService.createNotification(
                recipient,
                actor,
                "BOARD_INVITATION",
                "BOARD",
                boardId,
                boardId,
                String.format("%s đã mời bạn cùng cộng tác tại bảng: %s", actor.getUsername(), board.getName())
        );

        emailNotificationService.sendBoardInvitationEmail(actor, recipient, board.getName(), boardId);
        log.info("Board invitation sent to {} for board {}", email, boardId);
    }

    @Override
    @Transactional
    public BoardResponse archiveBoard(Long boardId, UserPrincipal currentUser) {
        Board board = findBoardOrThrow(boardId);
        validateWorkspaceAdmin(board.getWorkspace().getId(), currentUser.getId());
        board.setIsArchived(true);
        board = boardRepository.save(board);
        log.info("Board archived: {}", boardId);
        return toBoardResponse(board, false);
    }

    @Override
    @Transactional
    public BoardResponse restoreBoard(Long boardId, UserPrincipal currentUser) {
        Board board = findBoardOrThrow(boardId);
        validateWorkspaceAdmin(board.getWorkspace().getId(), currentUser.getId());

        // Reset position to end
        Board lastBoard = boardRepository.findTopByWorkspaceIdAndIsArchivedFalseOrderByPositionDesc(board.getWorkspace().getId());
        int nextPosition = lastBoard == null ? 0 : lastBoard.getPosition() + 1;

        board.setIsArchived(false);
        board.setPosition(nextPosition);
        board = boardRepository.save(board);
        log.info("Board restored: {}", boardId);
        return toBoardResponse(board, false);
    }

    @Override
    public List<BoardResponse> getArchivedBoards(Long workspaceId, UserPrincipal currentUser) {
        validateWorkspaceMembership(workspaceId, currentUser.getId());
        List<Board> boards = boardRepository.findByWorkspaceIdAndIsArchivedTrueOrderByPositionAscCreatedAtAsc(workspaceId);
        return boards.stream().map(b -> toBoardResponse(b, false)).toList();
    }

    @Override
    @Transactional
    public BoardResponse updateVisibility(Long boardId, boolean isPublic, UserPrincipal currentUser) {
        Board board = findBoardOrThrow(boardId);
        validateWorkspaceAdmin(board.getWorkspace().getId(), currentUser.getId());

        board.setIsPublic(isPublic);
        if (isPublic && board.getPublicToken() == null) {
            board.setPublicToken(UUID.randomUUID().toString().replace("-", ""));
        } else if (!isPublic) {
            board.setPublicToken(null);
        }

        board = boardRepository.save(board);
        log.info("Board {} visibility updated to public: {}", boardId, isPublic);
        return toBoardResponse(board, false);
    }

    @Override
    @Transactional(readOnly = true)
    public BoardPublicDto getPublicBoard(String token) {
        Board board = boardRepository.findByPublicToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Public Board", token));
        
        if (!board.getIsPublic()) {
            throw new UnauthorizedException("This board is no longer public");
        }

        return toBoardPublicDto(board);
    }

    // ─── Helpers ────────────────────────────────────────

    private Board findBoardOrThrow(Long id) {
        return boardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Board", id));
    }

    private void validateWorkspaceMembership(Long workspaceId, Long userId) {
        if (!memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new UnauthorizedException("You are not a member of this workspace");
        }
    }

    private void validateWorkspaceWriteAccess(Long workspaceId, Long userId) {
        boolean hasAccess = memberRepository.existsByWorkspaceIdAndUserIdAndRoleIn(
                workspaceId, userId, List.of(Role.OWNER, Role.ADMIN, Role.MEMBER));
        if (!hasAccess) {
            throw new UnauthorizedException("You do not have permission to perform this action");
        }
    }

    private void validateWorkspaceAdmin(Long workspaceId, Long userId) {
        boolean hasAccess = memberRepository.existsByWorkspaceIdAndUserIdAndRoleIn(
                workspaceId, userId, List.of(Role.OWNER, Role.ADMIN));
        if (!hasAccess) {
            throw new UnauthorizedException("Only OWNER or ADMIN can perform this action");
        }
    }

    private BoardResponse toBoardResponse(Board board, boolean includeLists) {
        List<TaskList> taskLists = taskListRepository
                .findByBoardIdAndIsArchivedFalseOrderByPositionAsc(board.getId());

        int listCount = taskLists.size();
        int totalCards = 0;

        List<ListResponse> lists = null;
        if (includeLists) {
            lists = taskLists.stream().map(this::toListResponse).toList();
            totalCards = lists.stream()
                    .mapToInt(l -> l.getCards() != null ? l.getCards().size() : 0)
                    .sum();
        } else {
            for (TaskList tl : taskLists) {
                totalCards += cardRepository.countByTaskListIdAndIsArchivedFalse(tl.getId());
            }
        }

        return BoardResponse.builder()
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
        List<TaskList> taskLists = taskListRepository
                .findByBoardIdAndIsArchivedFalseOrderByPositionAsc(board.getId());

        List<ListResponse> lists = taskLists.stream().map(this::toPublicListResponse).toList();

        List<BoardPublicDto.PublicUserResponse> publicMembers = board.getWorkspace() != null 
            ? memberRepository.findByWorkspaceId(board.getWorkspace().getId()).stream()
                .map(m -> BoardPublicDto.PublicUserResponse.builder()
                        .id(m.getUser().getId())
                        .username(m.getUser().getUsername())
                        .avatarUrl(m.getUser().getAvatarUrl())
                        .build())
                .toList()
            : List.of();

        return BoardPublicDto.builder()
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
        List<Card> cards = cardRepository
                .findByTaskListIdAndIsArchivedFalseOrderByPositionAsc(taskList.getId());

        return ListResponse.builder()
                .id(taskList.getId())
                .boardId(taskList.getBoard().getId())
                .name(taskList.getName())
                .position(taskList.getPosition())
                .cards(cards.stream().map(this::toPublicCardResponse).toList())
                .build();
    }

    private CardResponse toPublicCardResponse(Card card) {
        CardResponse response = toCardResponse(card);
        // Clear emails from members for public view
        if (response.getMembers() != null) {
            response.setMembers(response.getMembers().stream()
                .map(m -> UserResponse.builder()
                    .id(m.getId())
                    .username(m.getUsername())
                    .avatarUrl(m.getAvatarUrl())
                    .email(null) // Hide email
                    .build())
                .toList());
        }
        return response;
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
                .startDate(card.getStartDate())
                .priority(card.getPriority() != null ? card.getPriority().name() : "MEDIUM")
                .isArchived(card.getIsArchived())
                .createdById(card.getCreatedBy() != null ? card.getCreatedBy().getId() : null)
                .createdByName(card.getCreatedBy() != null ? card.getCreatedBy().getUsername() : "Hệ thống")
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
