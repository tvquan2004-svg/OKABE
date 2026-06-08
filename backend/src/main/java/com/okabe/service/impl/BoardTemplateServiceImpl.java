package com.okabe.service.impl;

import com.okabe.dto.request.SaveAsTemplateRequest;
import com.okabe.dto.response.BoardTemplateResponse;
import com.okabe.dto.response.TemplateCardResponse;
import com.okabe.dto.response.TemplateListResponse;
import com.okabe.entity.*;
import com.okabe.entity.enums.Role;
import com.okabe.exception.ResourceNotFoundException;
import com.okabe.exception.UnauthorizedException;
import com.okabe.repository.*;
import com.okabe.security.UserPrincipal;
import com.okabe.service.BoardTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardTemplateServiceImpl implements BoardTemplateService {

    private final BoardTemplateRepository templateRepository;
    private final BoardRepository boardRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final TaskListRepository taskListRepository;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;

    @Override
    public List<BoardTemplateResponse> getAllTemplates(Long workspaceId, UserPrincipal currentUser) {
        if (workspaceId != null) { // Nếu có workspace ID
            validateWorkspaceMembership(workspaceId, currentUser.getId()); // Kiểm tra quyền thành viên
        }
        
        List<BoardTemplate> templates = templateRepository.findAllSystemOrByWorkspace(workspaceId); // Lấy template hệ thống và workspace
        return templates.stream().map(this::toResponse).toList(); // Chuyển đổi và trả về
    }

    @Override
    public BoardTemplateResponse getTemplate(Long templateId, UserPrincipal currentUser) {
        BoardTemplate template = findTemplateOrThrow(templateId); // Tìm template theo ID
        if (!template.getIsSystem() && template.getWorkspace() != null) { // Nếu không phải template hệ thống
            validateWorkspaceMembership(template.getWorkspace().getId(), currentUser.getId()); // Kiểm tra quyền
        }
        return toResponse(template); // Trả về phản hồi
    }

    @Override
    @Transactional
    public void deleteTemplate(Long templateId, UserPrincipal currentUser) {
        BoardTemplate template = findTemplateOrThrow(templateId); // Tìm template
        
        if (template.getIsSystem()) { // Nếu là template hệ thống
            throw new UnauthorizedException("System templates cannot be deleted"); // Ném lỗi
        }
        
        if (template.getWorkspace() != null) { // Nếu thuộc workspace
            validateWorkspaceAdmin(template.getWorkspace().getId(), currentUser.getId()); // Kiểm tra quyền admin
        } else if (template.getCreatedBy() != null && !template.getCreatedBy().getId().equals(currentUser.getId())) { // Nếu không phải người tạo
            throw new UnauthorizedException("You are not the creator of this template"); // Ném lỗi
        }

        templateRepository.delete(template); // Xóa template
        log.info("Board template deleted: {}", templateId); // Ghi log
    }

    @Override
    @Transactional
    public BoardTemplateResponse saveAsTemplate(Long boardId, SaveAsTemplateRequest request, UserPrincipal currentUser) {
        Board board = boardRepository.findById(boardId) // Tìm bảng theo ID
                .orElseThrow(() -> new ResourceNotFoundException("Board", boardId)); // Ném lỗi nếu không tìm thấy
        
        validateWorkspaceAdmin(board.getWorkspace().getId(), currentUser.getId()); // Kiểm tra quyền admin

        User creator = userRepository.findById(currentUser.getId()).orElseThrow(); // Lấy thông tin người tạo

        BoardTemplate template = BoardTemplate.builder()
                .name(request.getName()) // Gán tên template
                .description(request.getDescription()) // Gán mô tả
                .isSystem(false) // Đánh dấu không phải hệ thống
                .createdBy(creator) // Gán người tạo
                .workspace(board.getWorkspace()) // Gán workspace
                .lists(new ArrayList<>()) // Khởi tạo danh sách rỗng
                .build(); // Xây dựng BoardTemplate

        List<TaskList> boardLists = taskListRepository.findByBoardIdAndIsArchivedFalseOrderByPositionAsc(boardId); // Lấy danh sách cột
        
        for (TaskList taskList : boardLists) { // Duyệt từng cột
            TemplateList templateList = TemplateList.builder()
                    .template(template) // Gán template cha
                    .name(taskList.getName()) // Gán tên cột
                    .position(taskList.getPosition()) // Gán vị trí
                    .cards(new ArrayList<>()) // Khởi tạo danh sách card
                    .build(); // Xây dựng TemplateList
            
            List<Card> boardCards = cardRepository.findByTaskListIdAndIsArchivedFalseOrderByPositionAsc(taskList.getId()); // Lấy card trong cột
            for (Card card : boardCards) { // Duyệt từng card
                TemplateCard templateCard = TemplateCard.builder()
                        .templateList(templateList) // Gán template list cha
                        .title(card.getTitle()) // Gán tiêu đề
                        .description(card.getDescription()) // Gán mô tả
                        .position(card.getPosition()) // Gán vị trí
                        .build(); // Xây dựng TemplateCard
                templateList.getCards().add(templateCard); // Thêm vào danh sách
            }
            template.getLists().add(templateList); // Thêm template list vào template
        }

        template = templateRepository.save(template); // Lưu template vào CSDL
        log.info("Board {} saved as template: {}", boardId, template.getName()); // Ghi log
        return toResponse(template); // Trả về phản hồi
    }

    // ─── Helpers ────────────────────────────────────────

    private BoardTemplate findTemplateOrThrow(Long id) {
        return templateRepository.findById(id) // Tìm template theo ID
                .orElseThrow(() -> new ResourceNotFoundException("BoardTemplate", id)); // Ném lỗi nếu không tìm thấy
    }

    private void validateWorkspaceMembership(Long workspaceId, Long userId) {
        if (!memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) { // Nếu không phải thành viên
            throw new UnauthorizedException("You are not a member of this workspace"); // Ném lỗi
        }
    }

    private void validateWorkspaceAdmin(Long workspaceId, Long userId) {
        boolean hasAccess = memberRepository.existsByWorkspaceIdAndUserIdAndRoleIn( // Kiểm tra quyền admin
                workspaceId, userId, List.of(Role.OWNER, Role.ADMIN));
        if (!hasAccess) { // Nếu không có quyền
            throw new UnauthorizedException("Only OWNER or ADMIN can perform this action"); // Ném lỗi
        }
    }

    private BoardTemplateResponse toResponse(BoardTemplate template) {
        return BoardTemplateResponse.builder()
                .id(template.getId()) // Gán ID template
                .name(template.getName()) // Gán tên
                .description(template.getDescription()) // Gán mô tả
                .isSystem(template.getIsSystem()) // Gán trạng thái hệ thống
                .lists(template.getLists() != null ? template.getLists().stream() // Gán danh sách cột
                        .map(this::toListResponse).collect(Collectors.toList()) : null)
                .build(); // Xây dựng BoardTemplateResponse
    }

    private TemplateListResponse toListResponse(TemplateList list) {
        return TemplateListResponse.builder()
                .id(list.getId()) // Gán ID
                .name(list.getName()) // Gán tên
                .position(list.getPosition()) // Gán vị trí
                .cards(list.getCards() != null ? list.getCards().stream() // Gán danh sách card
                        .map(this::toCardResponse).collect(Collectors.toList()) : null)
                .build(); // Xây dựng TemplateListResponse
    }

    private TemplateCardResponse toCardResponse(TemplateCard card) {
        return TemplateCardResponse.builder()
                .id(card.getId()) // Gán ID
                .title(card.getTitle()) // Gán tiêu đề
                .description(card.getDescription()) // Gán mô tả
                .position(card.getPosition()) // Gán vị trí
                .build(); // Xây dựng TemplateCardResponse
    }
}
