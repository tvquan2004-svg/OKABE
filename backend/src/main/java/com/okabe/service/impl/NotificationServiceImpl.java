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
        if (actor != null && recipient.getId().equals(actor.getId())) { // Nếu người nhận là chính người thực hiện
            return; // Không gửi thông báo
        }

        Notification notification = Notification.builder()
                .recipient(recipient) // Gán người nhận
                .actor(actor) // Gán người thực hiện hành động
                .type(type) // Gán loại thông báo
                .entityType(entityType) // Gán loại thực thể
                .entityId(entityId) // Gán ID thực thể
                .extraId(extraId) // Gán ID bổ sung
                .message(message) // Gán nội dung thông báo
                .build(); // Xây dựng đối tượng Notification

        notification = notificationRepository.save(notification); // Lưu thông báo vào CSDL
        log.info("Notification created for user {} (ID: {}): {}", recipient.getEmail(), recipient.getId(), message); // Ghi log
        
        // Broadcast via WebSocket
        webSocketService.sendToUser(recipient.getId(), "NOTIFICATION_RECEIVED", toResponse(notification)); // Gửi thông báo realtime
    }

    @Override
    public Page<NotificationResponse> getNotifications(UserPrincipal currentUser, Pageable pageable) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(currentUser.getId(), pageable) // Tìm thông báo theo người nhận, phân trang
                .map(this::toResponse); // Chuyển đổi sang NotificationResponse
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, UserPrincipal currentUser) {
        Notification notification = notificationRepository.findById(notificationId) // Tìm thông báo theo ID
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId)); // Ném lỗi nếu không tìm thấy

        if (!notification.getRecipient().getId().equals(currentUser.getId())) { // Nếu không phải người nhận
            throw new UnauthorizedException("You can only mark your own notifications as read"); // Ném lỗi
        }

        notification.setIsRead(true); // Đánh dấu đã đọc
        notificationRepository.save(notification); // Lưu thay đổi
    }

    @Override
    @Transactional
    public void markAllAsRead(UserPrincipal currentUser) {
        notificationRepository.markAllAsRead(currentUser.getId()); // Đánh dấu tất cả thông báo đã đọc
    }

    @Override
    public long getUnreadCount(UserPrincipal currentUser) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(currentUser.getId()); // Đếm số thông báo chưa đọc
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId()) // Gán ID thông báo
                .actorId(n.getActor() != null ? n.getActor().getId() : null) // Gán ID người thực hiện
                .actorName(n.getActor() != null ? n.getActor().getUsername() : "System") // Gán tên người thực hiện
                .actorAvatarUrl(n.getActor() != null ? n.getActor().getAvatarUrl() : null) // Gán URL ảnh đại diện
                .type(n.getType()) // Gán loại thông báo
                .entityType(n.getEntityType()) // Gán loại thực thể
                .entityId(n.getEntityId()) // Gán ID thực thể
                .extraId(n.getExtraId()) // Gán ID bổ sung
                .message(n.getMessage()) // Gán nội dung
                .isRead(n.getIsRead()) // Gán trạng thái đã đọc
                .createdAt(n.getCreatedAt()) // Gán thời gian tạo
                .build(); // Xây dựng NotificationResponse
    }
}
