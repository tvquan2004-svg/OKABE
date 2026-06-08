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
        if (query == null || query.isBlank()) return List.of(); // Trả về danh sách rỗng nếu query trống

        String keyword = query.trim().toLowerCase(); // Chuẩn hóa từ khóa
        List<Long> userWorkspaceIds = getUserWorkspaceIds(currentUser.getId()); // Lấy danh sách workspace ID

        if (userWorkspaceIds.isEmpty()) return List.of(); // Trả về rỗng nếu không có workspace

        Set<SearchResultItem> results = new LinkedHashSet<>(); // Khởi tạo tập kết quả

        results.addAll(searchWorkspaces(keyword, currentUser.getId(), userWorkspaceIds)); // Thêm kết quả workspace
        results.addAll(searchBoards(keyword, userWorkspaceIds)); // Thêm kết quả bảng
        results.addAll(searchCards(keyword, userWorkspaceIds)); // Thêm kết quả thẻ
        results.addAll(searchMembers(keyword, userWorkspaceIds)); // Thêm kết quả thành viên

        return new ArrayList<>(results); // Trả về danh sách kết quả
    }

    private List<Long> getUserWorkspaceIds(Long userId) {
        return workspaceMemberRepository.findByUserId(userId).stream() // Lấy danh sách workspace membership
                .map(WorkspaceMember::getWorkspaceId) // Lấy workspace ID
                .toList(); // Thu thập thành danh sách
    }

    private List<SearchResultItem> searchWorkspaces(String keyword, Long userId, List<Long> workspaceIds) {
        if (workspaceIds.isEmpty()) return List.of(); // Trả về rỗng nếu không có workspace
        return workspaceRepository.findByIdIn(workspaceIds).stream() // Tìm workspace theo IDs
                .filter(w -> matches(w.getName(), keyword) || matches(w.getDescription(), keyword)) // Lọc theo từ khóa
                .map(w -> SearchResultItem.builder()
                        .id("workspace_" + w.getId()) // Tạo ID duy nhất
                        .type("workspace") // Loại workspace
                        .title(w.getName()) // Tiêu đề
                        .subtitle(w.getDescription()) // Mô tả
                        .breadcrumb("Workspace") // Đường dẫn
                        .url("/workspace/" + w.getId()) // URL
                        .icon("workspace") // Icon
                        .build()) // Xây dựng SearchResultItem
                .toList(); // Thu thập thành danh sách
    }

    private List<SearchResultItem> searchBoards(String keyword, List<Long> workspaceIds) {
        if (workspaceIds.isEmpty()) return List.of(); // Trả về rỗng nếu không có workspace
        return boardRepository.findByWorkspaceIdIn(workspaceIds).stream() // Tìm bảng theo workspace IDs
                .filter(b -> matches(b.getName(), keyword) || matches(b.getDescription(), keyword)) // Lọc theo từ khóa
                .map(b -> SearchResultItem.builder()
                        .id("board_" + b.getId()) // ID duy nhất
                        .type("board") // Loại board
                        .title(b.getName()) // Tên bảng
                        .subtitle(b.getDescription()) // Mô tả
                        .breadcrumb(getWorkspaceName(b.getWorkspace().getId())) // Tên workspace
                        .url("/board/" + b.getId()) // URL
                        .icon("board") // Icon
                        .build()) // Xây dựng SearchResultItem
                .toList(); // Thu thập thành danh sách
    }

    private List<SearchResultItem> searchCards(String keyword, List<Long> workspaceIds) {
        if (workspaceIds.isEmpty()) return List.of(); // Trả về rỗng nếu không có workspace
        return cardRepository.findByWorkspaceIdIn(workspaceIds).stream() // Tìm card theo workspace IDs
                .filter(c -> matches(c.getTitle(), keyword) || matches(c.getDescription(), keyword)) // Lọc theo từ khóa
                .map(c -> SearchResultItem.builder()
                        .id("card_" + c.getId()) // ID duy nhất
                        .type("card") // Loại card
                        .title(c.getTitle()) // Tiêu đề
                        .subtitle(c.getDescription() != null && c.getDescription().length() > 100 // Mô tả (cắt ngắn nếu dài)
                                ? c.getDescription().substring(0, 100) : c.getDescription())
                        .breadcrumb(getBoardName(c) + " · " + getListName(c)) // Đường dẫn board · list
                        .url("/board/" + getBoardId(c) + "?cardId=" + c.getId()) // URL
                        .icon("card") // Icon
                        .build()) // Xây dựng SearchResultItem
                .toList(); // Thu thập thành danh sách
    }

    private List<SearchResultItem> searchMembers(String keyword, List<Long> workspaceIds) {
        if (workspaceIds.isEmpty()) return List.of(); // Trả về rỗng nếu không có workspace
        return workspaceMemberRepository.findByWorkspaceIdIn(workspaceIds).stream() // Lấy thành viên từ workspace
                .map(WorkspaceMember::getUser) // Lấy thông tin user
                .distinct() // Loại bỏ trùng lặp
                .filter(u -> matches(u.getUsername(), keyword) || matches(u.getEmail(), keyword)) // Lọc theo từ khóa
                .map(u -> SearchResultItem.builder()
                        .id("user_" + u.getId()) // ID duy nhất
                        .type("member") // Loại member
                        .title(u.getUsername()) // Tên người dùng
                        .subtitle(u.getEmail()) // Email
                        .breadcrumb("Member") // Breadcrumb
                        .url(null) // Không có URL
                        .icon("person") // Icon
                        .build()) // Xây dựng SearchResultItem
                .toList(); // Thu thập thành danh sách
    }

    private boolean matches(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword); // Kiểm tra value chứa keyword
    }

    private String getWorkspaceName(Long workspaceId) {
        return workspaceRepository.findById(workspaceId) // Tìm workspace theo ID
                .map(Workspace::getName).orElse(""); // Lấy tên hoặc chuỗi rỗng
    }

    private String getBoardName(Card card) {
        if (card.getTaskList() != null && card.getTaskList().getBoard() != null) { // Kiểm tra board tồn tại
            return card.getTaskList().getBoard().getName(); // Trả về tên board
        }
        return ""; // Chuỗi rỗng nếu không có
    }

    private Long getBoardId(Card card) {
        if (card.getTaskList() != null && card.getTaskList().getBoard() != null) { // Kiểm tra board tồn tại
            return card.getTaskList().getBoard().getId(); // Trả về ID board
        }
        return 0L; // 0 nếu không có
    }

    private String getListName(Card card) {
        if (card.getTaskList() != null) return card.getTaskList().getName(); // Trả về tên list
        return ""; // Chuỗi rỗng nếu không có
    }
}
