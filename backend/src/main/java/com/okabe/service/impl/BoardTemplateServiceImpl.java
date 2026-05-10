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
        if (workspaceId != null) {
            validateWorkspaceMembership(workspaceId, currentUser.getId());
        }
        
        List<BoardTemplate> templates = templateRepository.findAllSystemOrByWorkspace(workspaceId);
        return templates.stream().map(this::toResponse).toList();
    }

    @Override
    public BoardTemplateResponse getTemplate(Long templateId, UserPrincipal currentUser) {
        BoardTemplate template = findTemplateOrThrow(templateId);
        if (!template.getIsSystem() && template.getWorkspace() != null) {
            validateWorkspaceMembership(template.getWorkspace().getId(), currentUser.getId());
        }
        return toResponse(template);
    }

    @Override
    @Transactional
    public void deleteTemplate(Long templateId, UserPrincipal currentUser) {
        BoardTemplate template = findTemplateOrThrow(templateId);
        
        if (template.getIsSystem()) {
            throw new UnauthorizedException("System templates cannot be deleted");
        }
        
        if (template.getWorkspace() != null) {
            validateWorkspaceAdmin(template.getWorkspace().getId(), currentUser.getId());
        } else if (template.getCreatedBy() != null && !template.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You are not the creator of this template");
        }

        templateRepository.delete(template);
        log.info("Board template deleted: {}", templateId);
    }

    @Override
    @Transactional
    public BoardTemplateResponse saveAsTemplate(Long boardId, SaveAsTemplateRequest request, UserPrincipal currentUser) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board", boardId));
        
        validateWorkspaceAdmin(board.getWorkspace().getId(), currentUser.getId());

        User creator = userRepository.findById(currentUser.getId()).orElseThrow();

        BoardTemplate template = BoardTemplate.builder()
                .name(request.getName())
                .description(request.getDescription())
                .isSystem(false)
                .createdBy(creator)
                .workspace(board.getWorkspace())
                .lists(new ArrayList<>())
                .build();

        List<TaskList> boardLists = taskListRepository.findByBoardIdAndIsArchivedFalseOrderByPositionAsc(boardId);
        
        for (TaskList taskList : boardLists) {
            TemplateList templateList = TemplateList.builder()
                    .template(template)
                    .name(taskList.getName())
                    .position(taskList.getPosition())
                    .cards(new ArrayList<>())
                    .build();
            
            List<Card> boardCards = cardRepository.findByTaskListIdAndIsArchivedFalseOrderByPositionAsc(taskList.getId());
            for (Card card : boardCards) {
                TemplateCard templateCard = TemplateCard.builder()
                        .templateList(templateList)
                        .title(card.getTitle())
                        .description(card.getDescription())
                        .position(card.getPosition())
                        .build();
                templateList.getCards().add(templateCard);
            }
            template.getLists().add(templateList);
        }

        template = templateRepository.save(template);
        log.info("Board {} saved as template: {}", boardId, template.getName());
        return toResponse(template);
    }

    // ─── Helpers ────────────────────────────────────────

    private BoardTemplate findTemplateOrThrow(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BoardTemplate", id));
    }

    private void validateWorkspaceMembership(Long workspaceId, Long userId) {
        if (!memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new UnauthorizedException("You are not a member of this workspace");
        }
    }

    private void validateWorkspaceAdmin(Long workspaceId, Long userId) {
        boolean hasAccess = memberRepository.existsByWorkspaceIdAndUserIdAndRoleIn(
                workspaceId, userId, List.of(Role.OWNER, Role.ADMIN));
        if (!hasAccess) {
            throw new UnauthorizedException("Only OWNER or ADMIN can perform this action");
        }
    }

    private BoardTemplateResponse toResponse(BoardTemplate template) {
        return BoardTemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .description(template.getDescription())
                .isSystem(template.getIsSystem())
                .lists(template.getLists() != null ? template.getLists().stream()
                        .map(this::toListResponse).collect(Collectors.toList()) : null)
                .build();
    }

    private TemplateListResponse toListResponse(TemplateList list) {
        return TemplateListResponse.builder()
                .id(list.getId())
                .name(list.getName())
                .position(list.getPosition())
                .cards(list.getCards() != null ? list.getCards().stream()
                        .map(this::toCardResponse).collect(Collectors.toList()) : null)
                .build();
    }

    private TemplateCardResponse toCardResponse(TemplateCard card) {
        return TemplateCardResponse.builder()
                .id(card.getId())
                .title(card.getTitle())
                .description(card.getDescription())
                .position(card.getPosition())
                .build();
    }
}
