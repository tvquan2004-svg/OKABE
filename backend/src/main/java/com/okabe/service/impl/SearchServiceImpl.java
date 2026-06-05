package com.okabe.service.impl;

import com.okabe.dto.response.SearchResultItem;
import com.okabe.entity.Board;
import com.okabe.entity.Card;
import com.okabe.entity.User;
import com.okabe.entity.Workspace;
import com.okabe.entity.WorkspaceMember;
import com.okabe.repository.*;
import com.okabe.security.UserPrincipal;
import com.okabe.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchServiceImpl implements SearchService {

    private final WorkspaceRepository workspaceRepository;
    private final BoardRepository boardRepository;
    private final CardRepository cardRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    @Override
    public List<SearchResultItem> globalSearch(String query, UserPrincipal currentUser) {
        if (query == null || query.isBlank()) return List.of();

        String keyword = query.trim().toLowerCase();
        List<Long> userWorkspaceIds = getUserWorkspaceIds(currentUser.getId());

        if (userWorkspaceIds.isEmpty()) return List.of();

        Set<SearchResultItem> results = new LinkedHashSet<>();

        results.addAll(searchWorkspaces(keyword, currentUser.getId(), userWorkspaceIds));
        results.addAll(searchBoards(keyword, userWorkspaceIds));
        results.addAll(searchCards(keyword, userWorkspaceIds));
        results.addAll(searchMembers(keyword, userWorkspaceIds));

        return new ArrayList<>(results);
    }

    private List<Long> getUserWorkspaceIds(Long userId) {
        return workspaceMemberRepository.findByUserId(userId).stream()
                .map(WorkspaceMember::getWorkspaceId)
                .toList();
    }

    private List<SearchResultItem> searchWorkspaces(String keyword, Long userId, List<Long> workspaceIds) {
        if (workspaceIds.isEmpty()) return List.of();
        return workspaceRepository.findByIdIn(workspaceIds).stream()
                .filter(w -> matches(w.getName(), keyword) || matches(w.getDescription(), keyword))
                .map(w -> SearchResultItem.builder()
                        .id("workspace_" + w.getId())
                        .type("workspace")
                        .title(w.getName())
                        .subtitle(w.getDescription())
                        .breadcrumb("Workspace")
                        .url("/workspace/" + w.getId())
                        .icon("workspace")
                        .build())
                .toList();
    }

    private List<SearchResultItem> searchBoards(String keyword, List<Long> workspaceIds) {
        if (workspaceIds.isEmpty()) return List.of();
        return boardRepository.findByWorkspaceIdIn(workspaceIds).stream()
                .filter(b -> matches(b.getName(), keyword) || matches(b.getDescription(), keyword))
                .map(b -> SearchResultItem.builder()
                        .id("board_" + b.getId())
                        .type("board")
                        .title(b.getName())
                        .subtitle(b.getDescription())
                        .breadcrumb(getWorkspaceName(b.getWorkspace().getId()))
                        .url("/board/" + b.getId())
                        .icon("board")
                        .build())
                .toList();
    }

    private List<SearchResultItem> searchCards(String keyword, List<Long> workspaceIds) {
        if (workspaceIds.isEmpty()) return List.of();
        return cardRepository.findByWorkspaceIdIn(workspaceIds).stream()
                .filter(c -> matches(c.getTitle(), keyword) || matches(c.getDescription(), keyword))
                .map(c -> SearchResultItem.builder()
                        .id("card_" + c.getId())
                        .type("card")
                        .title(c.getTitle())
                        .subtitle(c.getDescription() != null && c.getDescription().length() > 100
                                ? c.getDescription().substring(0, 100) : c.getDescription())
                        .breadcrumb(getBoardName(c) + " · " + getListName(c))
                        .url("/board/" + getBoardId(c) + "?cardId=" + c.getId())
                        .icon("card")
                        .build())
                .toList();
    }

    private List<SearchResultItem> searchMembers(String keyword, List<Long> workspaceIds) {
        if (workspaceIds.isEmpty()) return List.of();
        return workspaceMemberRepository.findByWorkspaceIdIn(workspaceIds).stream()
                .map(WorkspaceMember::getUser)
                .distinct()
                .filter(u -> matches(u.getUsername(), keyword) || matches(u.getEmail(), keyword))
                .map(u -> SearchResultItem.builder()
                        .id("user_" + u.getId())
                        .type("member")
                        .title(u.getUsername())
                        .subtitle(u.getEmail())
                        .breadcrumb("Member")
                        .url(null)
                        .icon("person")
                        .build())
                .toList();
    }

    private boolean matches(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private String getWorkspaceName(Long workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .map(Workspace::getName).orElse("");
    }

    private String getBoardName(Card card) {
        if (card.getTaskList() != null && card.getTaskList().getBoard() != null) {
            return card.getTaskList().getBoard().getName();
        }
        return "";
    }

    private Long getBoardId(Card card) {
        if (card.getTaskList() != null && card.getTaskList().getBoard() != null) {
            return card.getTaskList().getBoard().getId();
        }
        return 0L;
    }

    private String getListName(Card card) {
        if (card.getTaskList() != null) return card.getTaskList().getName();
        return "";
    }
}
