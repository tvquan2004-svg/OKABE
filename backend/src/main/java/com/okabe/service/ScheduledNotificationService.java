package com.okabe.service;

import com.okabe.entity.Card;
import com.okabe.entity.User;
import com.okabe.repository.CardRepository;
import com.okabe.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledNotificationService {

    private final CardRepository cardRepository;
    private final NotificationRepository notificationRepository;
    private final EmailNotificationService emailNotificationService;
    private final NotificationService notificationService;

    /**
     * Runs every minute to check for cards due within the next 24 hours and send email + in-app notifications.
     * Uses notificationSent flag to prevent duplicate notifications (each card only notified once per due date).
     */
    @Scheduled(cron = "0 * * * * *")  // Every minute
    @Transactional
    public void checkCardsDueSoon() {
        log.info("[Scheduler] Starting due-date notification check...");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime in24Hours = now.plusHours(24);

        // --- 1. Cards due within 24 hours (not yet notified) ---
        List<Card> dueSoonCards = cardRepository
                .findByIsArchivedFalseAndDueDateBetweenAndNotificationSentFalse(now, in24Hours);

        log.info("[Scheduler] Found {} card(s) due soon (within 24h)", dueSoonCards.size());

        for (Card card : dueSoonCards) {
            String message = String.format("Nhắc nhở: Thẻ \"%s\" sắp đến hạn vào %s",
                    card.getTitle(),
                    card.getDueDate().toLocalDate().toString());
            notifyUsersForCard(card, "CARD_DUE_SOON", message);
            card.setNotificationSent(true);
        }

        if (!dueSoonCards.isEmpty()) {
            cardRepository.saveAll(dueSoonCards);
            log.info("[Scheduler] Marked {} card(s) as notification sent (due soon)", dueSoonCards.size());
        }

        // --- 2. Overdue cards (past due, not yet notified) ---
        List<Card> overdueCards = cardRepository
                .findByIsArchivedFalseAndDueDateBeforeAndNotificationSentFalse(now);

        log.info("[Scheduler] Found {} overdue card(s)", overdueCards.size());

        for (Card card : overdueCards) {
            String message = String.format("Thẻ \"%s\" đã quá hạn!", card.getTitle());
            notifyUsersForCard(card, "CARD_OVERDUE", message);
            card.setNotificationSent(true);
        }

        if (!overdueCards.isEmpty()) {
            cardRepository.saveAll(overdueCards);
            log.info("[Scheduler] Marked {} overdue card(s) as notification sent", overdueCards.size());
        }

        log.info("[Scheduler] Due-date notification check completed.");
    }

    /**
     * Sends in-app + email notifications to all assigned members and the card creator.
     */
    private void notifyUsersForCard(Card card, String type, String message) {
        Long boardId = card.getTaskList().getBoard().getId();
        Long cardId = card.getId();

        // Notify all assigned members
        for (User member : card.getMembers()) {
            sendNotifications(member, card, type, message, boardId, cardId);
        }

        // Also notify creator if they are not an assigned member
        User creator = card.getCreatedBy();
        if (creator != null && card.getMembers().stream().noneMatch(m -> m.getId().equals(creator.getId()))) {
            sendNotifications(creator, card, type, message, boardId, cardId);
        }
    }

    /**
     * Runs daily at 3:00 AM to clean up old read notifications (older than 30 days).
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldNotifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        int deleted = notificationRepository.deleteReadOlderThan(cutoff);
        if (deleted > 0) {
            log.info("[Scheduler] Cleaned up {} read notifications older than 30 days", deleted);
        }
    }

    private void sendNotifications(User recipient, Card card, String type, String message,
                                   Long boardId, Long cardId) {
        // In-app notification
        try {
            notificationService.createNotification(recipient, null, type, "CARD", cardId, boardId, message);
        } catch (Exception e) {
            log.error("[Scheduler] Failed to create in-app notification for user {} on card {}: {}",
                    recipient.getId(), cardId, e.getMessage());
        }

        // Email notification — use different template based on type
        try {
            if ("CARD_OVERDUE".equals(type)) {
                emailNotificationService.sendOverdueEmail(
                        recipient, card.getTitle(), boardId, cardId, card.getDueDate());
            } else {
                emailNotificationService.sendDueSoonEmail(
                        recipient, card.getTitle(), boardId, cardId, card.getDueDate());
            }
        } catch (Exception e) {
            log.error("[Scheduler] Failed to queue email for user {} on card {}: {}",
                    recipient.getId(), cardId, e.getMessage());
        }
    }
}
