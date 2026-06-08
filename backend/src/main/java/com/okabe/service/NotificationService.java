package com.okabe.service;

import com.okabe.dto.response.NotificationResponse;
import com.okabe.entity.User;
import com.okabe.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    // Tạo thông báo mới cho người nhận
    void createNotification(User recipient, User actor, String type, String entityType, Long entityId, Long extraId, String message);

    // Lấy danh sách thông báo của user (có phân trang)
    Page<NotificationResponse> getNotifications(UserPrincipal currentUser, Pageable pageable);

    // Đánh dấu thông báo là đã đọc
    void markAsRead(Long notificationId, UserPrincipal currentUser);

    // Đánh dấu tất cả thông báo là đã đọc
    void markAllAsRead(UserPrincipal currentUser);

    // Lấy số lượng thông báo chưa đọc
    long getUnreadCount(UserPrincipal currentUser);
}
