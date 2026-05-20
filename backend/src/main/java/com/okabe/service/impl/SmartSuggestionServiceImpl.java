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
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, currentUser.getId())) {
            return List.of();
        }

        Set<String> dismissedKeys = dismissedSuggestionRepository
                .findByUserIdAndWorkspaceId(currentUser.getId(), workspaceId)
                .stream()
                .map(d -> d.getType() + ":" + d.getCardId())
                .collect(Collectors.toSet());

        List<SuggestionResponse> suggestions = new ArrayList<>();

        suggestions.addAll(generateStaleCardSuggestions(workspaceId, dismissedKeys));
        suggestions.addAll(generateDueSoonSuggestions(workspaceId, dismissedKeys));
        suggestions.addAll(generateIncompleteCardSuggestions(workspaceId, dismissedKeys));
        suggestions.addAll(generateOverloadedMemberSuggestions(workspaceId, dismissedKeys));

        long id = 1L;
        for (SuggestionResponse s : suggestions) {
            s.setId(id++);
        }

        return suggestions;
    }

    @Override
    @Transactional
    public void dismissSuggestion(Long suggestionId, UserPrincipal currentUser) {
        throw new UnsupportedOperationException("Use type + cardId based dismiss instead");
    }

    @Transactional
    public void dismissSuggestion(String type, Long cardId, Long workspaceId, UserPrincipal currentUser) {
        DismissedSuggestion dismissed = DismissedSuggestion.builder()
                .userId(currentUser.getId())
                .workspaceId(workspaceId)
                .type(type)
                .cardId(cardId)
                .build();
        dismissedSuggestionRepository.save(dismissed);
    }

    private List<SuggestionResponse> generateStaleCardSuggestions(Long workspaceId, Set<String> dismissedKeys) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(14);
        List<Card> staleCards = cardRepository.findStaleCardsByWorkspace(workspaceId, threshold);

        return staleCards.stream()
                .filter(c -> !dismissedKeys.contains("STALE:" + c.getId()))
                .map(c -> SuggestionResponse.builder()
                        .type("STALE")
                        .message("Thẻ \"" + c.getTitle() + "\" không được cập nhật hơn 2 tuần")
                        .cardId(c.getId())
                        .actionUrl("/board/" + c.getTaskList().getBoard().getId() + "?cardId=" + c.getId())
                        .build())
                .collect(Collectors.toList());
    }

    private List<SuggestionResponse> generateDueSoonSuggestions(Long workspaceId, Set<String> dismissedKeys) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = now.plusHours(24);
        List<Card> dueCards = cardRepository.findDueSoonCardsByWorkspace(workspaceId, now, end);

        return dueCards.stream()
                .filter(c -> !dismissedKeys.contains("DUE_SOON:" + c.getId()))
                .map(c -> SuggestionResponse.builder()
                        .type("DUE_SOON")
                        .message("Thẻ \"" + c.getTitle() + "\" sắp đến hạn trong 24h")
                        .cardId(c.getId())
                        .actionUrl("/board/" + c.getTaskList().getBoard().getId() + "?cardId=" + c.getId())
                        .build())
                .collect(Collectors.toList());
    }

    private List<SuggestionResponse> generateIncompleteCardSuggestions(Long workspaceId, Set<String> dismissedKeys) {
        List<Card> cards = cardRepository.findByWorkspaceId(workspaceId);

        return cards.stream()
                .filter(c -> !dismissedKeys.contains("INCOMPLETE:" + c.getId()))
                .filter(c -> isIncomplete(c))
                .map(c -> SuggestionResponse.builder()
                        .type("INCOMPLETE")
                        .message("Thẻ \"" + c.getTitle() + "\" thiếu mô tả hoặc checklist")
                        .cardId(c.getId())
                        .actionUrl("/board/" + c.getTaskList().getBoard().getId() + "?cardId=" + c.getId())
                        .build())
                .collect(Collectors.toList());
    }

    private List<SuggestionResponse> generateOverloadedMemberSuggestions(Long workspaceId, Set<String> dismissedKeys) {
        List<Card> cards = cardRepository.findAllWithMembersByWorkspace(workspaceId);

        Map<Long, List<Card>> memberCards = new HashMap<>();
        for (Card c : cards) {
            if (c.getMembers() != null) {
                for (User member : c.getMembers()) {
                    memberCards.computeIfAbsent(member.getId(), k -> new ArrayList<>()).add(c);
                }
            }
        }

        return memberCards.entrySet().stream()
                .filter(e -> e.getValue().size() > 10)
                .filter(e -> !dismissedKeys.contains("OVERLOADED:" + e.getKey()))
                .map(e -> {
                    User member = e.getValue().get(0).getMembers().stream()
                            .filter(m -> m.getId().equals(e.getKey()))
                            .findFirst().orElse(null);
                    String name = member != null ? member.getUsername() : "Thành viên #" + e.getKey();
                    return SuggestionResponse.builder()
                            .type("OVERLOADED")
                            .message(name + " đang có " + e.getValue().size() + " thẻ đang mở")
                            .cardId(null)
                            .actionUrl("/workspace/" + workspaceId)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private boolean isIncomplete(Card card) {
        boolean noDescription = card.getDescription() == null || card.getDescription().isBlank();
        List<Checklist> checklists = card.getChecklists();
        boolean noChecklists = checklists == null || checklists.isEmpty();
        boolean allEmpty = checklists != null && checklists.stream()
                .allMatch(cl -> cl.getItems() == null || cl.getItems().isEmpty());
        boolean allCompleted = checklists != null && !checklists.isEmpty() && checklists.stream()
                .filter(cl -> cl.getItems() != null)
                .flatMap(cl -> cl.getItems().stream())
                .allMatch(ChecklistItem::getIsCompleted);
        return noDescription || noChecklists || allEmpty || allCompleted;
    }
}
