package com.okabe.service.impl;

import com.okabe.dto.response.NotificationResponse;
import com.okabe.entity.Notification;
import com.okabe.entity.User;
import com.okabe.exception.ResourceNotFoundException;
import com.okabe.exception.UnauthorizedException;
import com.okabe.repository.NotificationRepository;
import com.okabe.security.UserPrincipal;
import com.okabe.service.NotificationService;
import com.okabe.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final WebSocketService webSocketService;

    @Override
    @Transactional
    public void createNotification(User recipient, User actor, String type, String entityType, Long entityId, Long extraId, String message) {
        // Don't notify the actor of their own action
        if (actor != null && recipient.getId().equals(actor.getId())) {
            return;
        }

        Notification notification = Notification.builder()
                .recipient(recipient)
                .actor(actor)
                .type(type)
                .entityType(entityType)
                .entityId(entityId)
                .extraId(extraId)
                .message(message)
                .build();

        notification = notificationRepository.save(notification);
        log.info("Notification created for user {} (ID: {}): {}", recipient.getEmail(), recipient.getId(), message);
        
        // Broadcast via WebSocket
        webSocketService.sendToUser(recipient.getId(), "NOTIFICATION_RECEIVED", toResponse(notification));
    }

    @Override
    public Page<NotificationResponse> getNotifications(UserPrincipal currentUser, Pageable pageable) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(currentUser.getId(), pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, UserPrincipal currentUser) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));

        if (!notification.getRecipient().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You can only mark your own notifications as read");
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(UserPrincipal currentUser) {
        notificationRepository.markAllAsRead(currentUser.getId());
    }

    @Override
    public long getUnreadCount(UserPrincipal currentUser) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(currentUser.getId());
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .actorId(n.getActor() != null ? n.getActor().getId() : null)
                .actorName(n.getActor() != null ? n.getActor().getUsername() : "System")
                .actorAvatarUrl(n.getActor() != null ? n.getActor().getAvatarUrl() : null)
                .type(n.getType())
                .entityType(n.getEntityType())
                .entityId(n.getEntityId())
                .extraId(n.getExtraId())
                .message(n.getMessage())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
