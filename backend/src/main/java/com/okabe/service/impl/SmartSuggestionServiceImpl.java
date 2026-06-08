package com.okabe.service.impl;

import com.okabe.dto.response.SuggestionResponse;
import com.okabe.entity.Card;
import com.okabe.entity.Checklist;
import com.okabe.entity.ChecklistItem;
import com.okabe.entity.DismissedSuggestion;
import com.okabe.entity.User;
import com.okabe.repository.CardRepository;
import com.okabe.repository.DismissedSuggestionRepository;
import com.okabe.repository.WorkspaceMemberRepository;
import com.okabe.security.UserPrincipal;
import com.okabe.service.SmartSuggestionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SmartSuggestionServiceImpl implements SmartSuggestionService {

    private final CardRepository cardRepository;
    private final DismissedSuggestionRepository dismissedSuggestionRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    @Override
    public List<SuggestionResponse> getSuggestions(Long workspaceId, UserPrincipal currentUser) {
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, currentUser.getId())) { // Nếu không phải thành viên
            return List.of(); // Trả về danh sách rỗng
        }

        Set<String> dismissedKeys = dismissedSuggestionRepository // Lấy danh sách gợi ý đã tắt
                .findByUserIdAndWorkspaceId(currentUser.getId(), workspaceId) // Tìm theo user và workspace
                .stream() // Xử lý stream
                .map(d -> d.getType() + ":" + d.getCardId()) // Tạo key từ type và cardId
                .collect(Collectors.toSet()); // Thu thập thành Set

        List<SuggestionResponse> suggestions = new ArrayList<>(); // Khởi tạo danh sách gợi ý

        suggestions.addAll(generateStaleCardSuggestions(workspaceId, dismissedKeys)); // Gợi ý card không cập nhật lâu
        suggestions.addAll(generateDueSoonSuggestions(workspaceId, dismissedKeys)); // Gợi ý card sắp đến hạn
        suggestions.addAll(generateIncompleteCardSuggestions(workspaceId, dismissedKeys)); // Gợi ý card không đầy đủ
        suggestions.addAll(generateOverloadedMemberSuggestions(workspaceId, dismissedKeys)); // Gợi ý thành viên quá tải

        long id = 1L; // Khởi tạo ID
        for (SuggestionResponse s : suggestions) { // Duyệt danh sách gợi ý
            s.setId(id++); // Gán ID tuần tự
        }

        return suggestions; // Trả về danh sách gợi ý
    }

    @Override
    @Transactional
    public void dismissSuggestion(Long suggestionId, UserPrincipal currentUser) {
        throw new UnsupportedOperationException("Use type + cardId based dismiss instead"); // Ném lỗi không hỗ trợ
    }

    @Transactional
    public void dismissSuggestion(String type, Long cardId, Long workspaceId, UserPrincipal currentUser) {
        DismissedSuggestion dismissed = DismissedSuggestion.builder()
                .userId(currentUser.getId()) // Gán user ID
                .workspaceId(workspaceId) // Gán workspace ID
                .type(type) // Gán loại gợi ý
                .cardId(cardId) // Gán card ID
                .build(); // Xây dựng DismissedSuggestion
        dismissedSuggestionRepository.save(dismissed); // Lưu vào CSDL
    }

    private List<SuggestionResponse> generateStaleCardSuggestions(Long workspaceId, Set<String> dismissedKeys) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(14); // Ngưỡng 14 ngày
        List<Card> staleCards = cardRepository.findStaleCardsByWorkspace(workspaceId, threshold); // Tìm card không cập nhật

        return staleCards.stream() // Xử lý danh sách
                .filter(c -> !dismissedKeys.contains("STALE:" + c.getId())) // Lọc bỏ gợi ý đã tắt
                .map(c -> SuggestionResponse.builder()
                        .type("STALE") // Loại STALE
                        .message("Thẻ \"" + c.getTitle() + "\" không được cập nhật hơn 2 tuần") // Nội dung
                        .cardId(c.getId()) // ID thẻ
                        .actionUrl("/board/" + c.getTaskList().getBoard().getId() + "?cardId=" + c.getId()) // URL
                        .build()) // Xây dựng SuggestionResponse
                .collect(Collectors.toList()); // Thu thập thành danh sách
    }

    private List<SuggestionResponse> generateDueSoonSuggestions(Long workspaceId, Set<String> dismissedKeys) {
        LocalDateTime now = LocalDateTime.now(); // Thời gian hiện tại
        LocalDateTime end = now.plusHours(24); // 24h sau
        List<Card> dueCards = cardRepository.findDueSoonCardsByWorkspace(workspaceId, now, end); // Tìm card sắp hết hạn

        return dueCards.stream() // Xử lý danh sách
                .filter(c -> !dismissedKeys.contains("DUE_SOON:" + c.getId())) // Lọc bỏ gợi ý đã tắt
                .map(c -> SuggestionResponse.builder()
                        .type("DUE_SOON") // Loại DUE_SOON
                        .message("Thẻ \"" + c.getTitle() + "\" sắp đến hạn trong 24h") // Nội dung
                        .cardId(c.getId()) // ID thẻ
                        .actionUrl("/board/" + c.getTaskList().getBoard().getId() + "?cardId=" + c.getId()) // URL
                        .build()) // Xây dựng SuggestionResponse
                .collect(Collectors.toList()); // Thu thập thành danh sách
    }

    private List<SuggestionResponse> generateIncompleteCardSuggestions(Long workspaceId, Set<String> dismissedKeys) {
        List<Card> cards = cardRepository.findByWorkspaceId(workspaceId); // Lấy tất cả card trong workspace

        return cards.stream() // Xử lý danh sách
                .filter(c -> !dismissedKeys.contains("INCOMPLETE:" + c.getId())) // Lọc bỏ gợi ý đã tắt
                .filter(c -> isIncomplete(c)) // Lọc card không đầy đủ
                .map(c -> SuggestionResponse.builder()
                        .type("INCOMPLETE") // Loại INCOMPLETE
                        .message("Thẻ \"" + c.getTitle() + "\" thiếu mô tả hoặc checklist") // Nội dung
                        .cardId(c.getId()) // ID thẻ
                        .actionUrl("/board/" + c.getTaskList().getBoard().getId() + "?cardId=" + c.getId()) // URL
                        .build()) // Xây dựng SuggestionResponse
                .collect(Collectors.toList()); // Thu thập thành danh sách
    }

    private List<SuggestionResponse> generateOverloadedMemberSuggestions(Long workspaceId, Set<String> dismissedKeys) {
        List<Card> cards = cardRepository.findAllWithMembersByWorkspace(workspaceId); // Lấy tất cả card kèm thành viên

        Map<Long, List<Card>> memberCards = new HashMap<>(); // Map lưu card theo member
        for (Card c : cards) { // Duyệt từng card
            if (c.getMembers() != null) { // Nếu có thành viên
                for (User member : c.getMembers()) { // Duyệt từng thành viên
                    memberCards.computeIfAbsent(member.getId(), k -> new ArrayList<>()).add(c); // Thêm card vào danh sách
                }
            }
        }

        return memberCards.entrySet().stream() // Xử lý map
                .filter(e -> e.getValue().size() > 10) // Lọc thành viên có >10 card
                .filter(e -> !dismissedKeys.contains("OVERLOADED:" + e.getKey())) // Lọc bỏ gợi ý đã tắt
                .map(e -> { // Chuyển đổi
                    User member = e.getValue().get(0).getMembers().stream() // Tìm thông tin thành viên
                            .filter(m -> m.getId().equals(e.getKey()))
                            .findFirst().orElse(null);
                    String name = member != null ? member.getUsername() : "Thành viên #" + e.getKey(); // Lấy tên
                    return SuggestionResponse.builder()
                            .type("OVERLOADED") // Loại OVERLOADED
                            .message(name + " đang có " + e.getValue().size() + " thẻ đang mở") // Nội dung
                            .cardId(null) // Không có card ID
                            .actionUrl("/workspace/" + workspaceId) // URL workspace
                            .build(); // Xây dựng SuggestionResponse
                })
                .collect(Collectors.toList()); // Thu thập thành danh sách
    }

    private boolean isIncomplete(Card card) {
        boolean noDescription = card.getDescription() == null || card.getDescription().isBlank(); // Kiểm tra không có mô tả
        List<Checklist> checklists = card.getChecklists(); // Lấy danh sách checklist
        boolean noChecklists = checklists == null || checklists.isEmpty(); // Kiểm tra không có checklist
        boolean allEmpty = checklists != null && checklists.stream() // Kiểm tra tất cả checklist rỗng
                .allMatch(cl -> cl.getItems() == null || cl.getItems().isEmpty());
        boolean allCompleted = checklists != null && !checklists.isEmpty() && checklists.stream() // Kiểm tra tất cả items hoàn thành
                .filter(cl -> cl.getItems() != null)
                .flatMap(cl -> cl.getItems().stream())
                .allMatch(ChecklistItem::getIsCompleted);
        return noDescription || noChecklists || allEmpty || allCompleted; // Trả về true nếu card không đầy đủ
    }
}
