package com.okabe.service;

import com.okabe.dto.response.PrioritySuggestion;
import com.okabe.entity.Card;
import com.okabe.entity.enums.Priority;
import com.okabe.exception.ResourceNotFoundException;
import com.okabe.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiPriorityService {

    private final CardRepository cardRepository;

    private static final List<String> HIGH_IMPACT_KEYWORDS = List.of("bug", "hotfix", "urgent", "critical", "blocker", "emergency", "crash", "security", "production");

    public PrioritySuggestion suggestPriority(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card", cardId));

        int score = 0;
        List<String> reasons = new java.util.ArrayList<>();

        // Due date score
        if (card.getDueDate() != null) {
            long hoursUntilDue = LocalDateTime.now().until(card.getDueDate(), ChronoUnit.HOURS);
            if (hoursUntilDue < 0) {
                score += 3;
                reasons.add("Đã quá hạn");
            } else if (hoursUntilDue <= 24) {
                score += 2;
                reasons.add("Đến hạn trong " + hoursUntilDue + "h");
            } else if (hoursUntilDue <= 72) {
                score += 1;
                reasons.add("Đến hạn trong " + hoursUntilDue + "h");
            }
        }

        // Keyword match in title + description
        String text = (card.getTitle() + " " + (card.getDescription() != null ? card.getDescription() : "")).toLowerCase();
        for (String keyword : HIGH_IMPACT_KEYWORDS) {
            if (text.contains(keyword)) {
                score += 2;
                reasons.add("Từ khóa '" + keyword + "'");
                break;
            }
        }

        // Card creator is admin (createdBy != null)
        if (card.getCreatedBy() != null) {
            score += 1;
            reasons.add("Người tạo là admin");
        }

        // Map score to priority
        String suggestedPriority;
        if (score >= 4) {
            suggestedPriority = Priority.CRITICAL.name();
        } else if (score == 3) {
            suggestedPriority = Priority.HIGH.name();
        } else if (score == 2) {
            suggestedPriority = Priority.MEDIUM.name();
        } else {
            suggestedPriority = Priority.LOW.name();
        }

        String reason = String.join(" + ", reasons);
        log.info("[AI-PRIORITY] Card {}: score={}, suggested={}, reason='{}'", cardId, score, suggestedPriority, reason);

        return new PrioritySuggestion(suggestedPriority, score, reason);
    }
}
