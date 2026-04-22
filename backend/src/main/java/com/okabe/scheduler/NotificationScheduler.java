package com.okabe.scheduler;

import com.okabe.entity.Card;
import com.okabe.repository.CardRepository;
import com.okabe.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final CardRepository cardRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 * * * *") // Every hour
    @Transactional
    public void checkDueDates() {
        log.info("Running due date notification scheduler");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow = now.plusHours(24);

        // 1. Cards overdue
        List<Card> overdueCards = cardRepository
                .findByIsArchivedFalseAndDueDateBeforeAndNotificationSentFalse(now);
        
        for (Card card : overdueCards) {
            notifyCardMembers(card, "CARD_OVERDUE", "Card is overdue: " + card.getTitle());
            card.setNotificationSent(true);
        }

        // 2. Cards due soon (within 24 hours)
        List<Card> dueSoonCards = cardRepository
                .findByIsArchivedFalseAndDueDateBetweenAndNotificationSentFalse(now, tomorrow);
        
        for (Card card : dueSoonCards) {
            notifyCardMembers(card, "CARD_DUE_SOON", "Card is due within 24 hours: " + card.getTitle());
            card.setNotificationSent(true);
        }

        cardRepository.saveAll(overdueCards);
        cardRepository.saveAll(dueSoonCards);
    }

    private void notifyCardMembers(Card card, String type, String message) {
        Long boardId = card.getTaskList().getBoard().getId();
        String translatedMessage = message;
        if (type.equals("CARD_OVERDUE")) {
            translatedMessage = "Thẻ đã quá hạn: " + card.getTitle();
        } else if (type.equals("CARD_DUE_SOON")) {
            translatedMessage = "Thẻ sắp đến hạn (trong 24 giờ tới): " + card.getTitle();
        }

        final String finalMsg = translatedMessage;
        // Notify all assigned members
        card.getMembers().forEach(member -> {
            notificationService.createNotification(member, null, type, "CARD", card.getId(), boardId, finalMsg);
        });

        // Also notify creator if not in members
        if (card.getCreatedBy() != null && card.getMembers().stream().noneMatch(m -> m.getId().equals(card.getCreatedBy().getId()))) {
            notificationService.createNotification(card.getCreatedBy(), null, type, "CARD", card.getId(), boardId, finalMsg);
        }
    }
}
