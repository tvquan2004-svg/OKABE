package com.okabe.repository;

import com.okabe.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Tìm thông báo theo người nhận, sắp xếp theo thời gian tạo giảm dần (có phân trang)
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    // Đếm số thông báo chưa đọc của người nhận
    long countByRecipientIdAndIsReadFalse(Long recipientId);

    @Modifying
    // Đánh dấu tất cả thông báo của người nhận là đã đọc
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipient.id = :recipientId AND n.isRead = false")
    void markAllAsRead(Long recipientId);
}
