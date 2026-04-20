package com.okabe.service;

import com.okabe.entity.Card;
import com.okabe.entity.User;
import com.okabe.repository.CardRepository;
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
    private final EmailNotificationService emailNotificationService;
    private final NotificationService notificationService;

    /**
     * Chạy mỗi giờ để kiểm tra các thẻ sắp đến hạn trong vòng 24h tới.
     * 0 0 * * * * = Chạy vào đầu mỗi giờ.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional(readOnly = true)
    public void checkCardsDueSoon() {
        log.info("Checking for cards due soon...");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow = now.plusHours(24);

        // Tìm các card có dueDate trong khoảng (now, tomorrow) và chưa được đánh dấu là đã gửi thông báo due_soon
        // Lưu ý: Cần thêm cột notification_sent vào bảng cards hoặc xử lý tránh lặp.
        // Ở đây tạm thời lấy các card chưa hoàn thành.
        List<Card> dueSoonCards = cardRepository.findByIsArchivedFalseAndDueDateBetweenAndNotificationSentFalse(now, tomorrow);

        for (Card card : dueSoonCards) {
            for (User assignee : card.getMembers()) {
                // In-app notification
                notificationService.createNotification(
                        assignee,
                        null,
                        "CARD_DUE_SOON",
                        "CARD",
                        card.getId(),
                        String.format("Reminder: The card \"%s\" is due tomorrow (%s)", 
                                card.getTitle(), card.getDueDate())
                );

                // Email notification
                emailNotificationService.sendDueSoonEmail(
                        assignee,
                        card.getTitle(),
                        card.getTaskList().getBoard().getId(),
                        card.getId(),
                        card.getDueDate()
                );
            }
            // Mark as sent and persist
            card.setNotificationSent(true);
            cardRepository.save(card);
        }
    }
}
