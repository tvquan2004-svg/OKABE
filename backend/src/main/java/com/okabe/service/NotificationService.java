package com.okabe.service;

import com.okabe.dto.response.NotificationResponse;
import com.okabe.entity.User;
import com.okabe.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    void createNotification(User recipient, User actor, String type, String entityType, Long entityId, String message);

    Page<NotificationResponse> getNotifications(UserPrincipal currentUser, Pageable pageable);

    void markAsRead(Long notificationId, UserPrincipal currentUser);

    void markAllAsRead(UserPrincipal currentUser);

    long getUnreadCount(UserPrincipal currentUser);
}
