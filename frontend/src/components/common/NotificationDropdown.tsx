import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  useGetNotificationsQuery, 
  useMarkAsReadMutation, 
  useMarkAllAsReadMutation,
  type Notification 
} from '../../services/notificationApi';
import {
  useAcceptInvitationByIdMutation,
  useRejectInvitationByIdMutation,
} from '../../services/workspaceApi';
import styles from './NotificationDropdown.module.css';

interface NotificationDropdownProps {
  onClose: () => void;
  position: { top: number; right: number };
}

const NotificationDropdown: React.FC<NotificationDropdownProps> = ({ onClose, position }) => {
  const navigate = useNavigate();
  const [loadingInvitationId, setLoadingInvitationId] = useState<number | null>(null);
  const [actionedNotifications, setActionedNotifications] = useState<Set<number>>(new Set());
  const { data: notificationsRes, isLoading } = useGetNotificationsQuery(
    { page: 0, size: 20 },
    { pollingInterval: 5000 }
  );
  const [markAsRead] = useMarkAsReadMutation();
  const [markAllAsRead] = useMarkAllAsReadMutation();
  const [acceptInvitation] = useAcceptInvitationByIdMutation();
  const [rejectInvitation] = useRejectInvitationByIdMutation();

  const notifications = notificationsRes?.data.content ?? [];

  const handleNotificationClick = async (n: Notification) => {
    if (n.type === 'WORKSPACE_INVITATION') return; // handle via buttons
    
    if (!n.isRead) {
      await markAsRead(n.id);
    }
    
    // Navigate based on type and entity type
    if (n.entityType === 'CARD') {
      // extraId stores boardId, entityId stores cardId
      if (n.extraId) {
        navigate(`/board/${n.extraId}?cardId=${n.entityId}`);
      }
    } else if (n.entityType === 'BOARD') {
      navigate(`/board/${n.entityId}`);
    } else if (n.entityType === 'WORKSPACE') {
      navigate(`/workspace/${n.entityId}`);
    }
    
    onClose();
  };

  const handleAcceptInvitation = async (n: Notification) => {
    if (!n.extraId) return;
    setLoadingInvitationId(n.id);
    try {
      await acceptInvitation(n.extraId).unwrap();
      await markAsRead(n.id);
      setActionedNotifications(prev => new Set(prev).add(n.id));
    } catch {
      // ignore
    }
    setLoadingInvitationId(null);
  };

  const handleRejectInvitation = async (n: Notification) => {
    if (!n.extraId) return;
    setLoadingInvitationId(n.id);
    try {
      await rejectInvitation(n.extraId).unwrap();
      await markAsRead(n.id);
      setActionedNotifications(prev => new Set(prev).add(n.id));
    } catch {
      // ignore
    }
    setLoadingInvitationId(null);
  };

  const formatTime = (dateStr: string) => {
    const date = new Date(dateStr);
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    
    const minutes = Math.floor(diff / 60000);
    if (minutes < 1) return 'Vừa xong';
    if (minutes < 60) return `${minutes} phút trước`;
    
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours} giờ trước`;
    
    return date.toLocaleDateString('vi-VN');
  };

  return (
    <div className={styles.dropdownContainer} style={{ top: position.top, right: position.right }} onClick={(e) => e.stopPropagation()}>
      <div className={styles.header}>
        <h3>Thông báo</h3>
        {notifications.length > 0 && (
          <button className={styles.markAllBtn} onClick={() => markAllAsRead()}>
            Đánh dấu tất cả là đã đọc
          </button>
        )}
      </div>

      <div className={styles.list}>
        {isLoading ? (
          <div className={styles.empty}>Đang tải...</div>
        ) : notifications.length === 0 ? (
          <div className={styles.empty}>
            <span className={styles.emptyIcon}>🔔</span>
            <p>Chưa có thông báo nào</p>
          </div>
        ) : (
          notifications.map((n) => (
            <div 
              key={n.id} 
              className={`${styles.notificationItem} ${!n.isRead ? styles.unread : ''}`}
              onClick={() => handleNotificationClick(n)}
            >
              {!n.isRead && <div className={styles.unreadDot} />}
              <div className={styles.actorAvatar}>
                {n.actorAvatarUrl ? (
                  <img src={n.actorAvatarUrl} alt={n.actorName} />
                ) : (
                  n.actorName.charAt(0).toUpperCase()
                )}
              </div>
              <div className={styles.content}>
                <p className={styles.message}>{n.message}</p>
                <span className={styles.time}>{formatTime(n.createdAt)}</span>
                {n.type === 'WORKSPACE_INVITATION' && (
                  <div className={styles.inviteActions}>
                    {actionedNotifications.has(n.id) ? (
                      <span className={styles.actionedLabel}>✓ Đã xử lý</span>
                    ) : (
                      <>
                        <button
                          className={styles.acceptBtn}
                          disabled={loadingInvitationId === n.id}
                          onClick={(e) => { e.stopPropagation(); void handleAcceptInvitation(n); }}
                        >
                          {loadingInvitationId === n.id ? '...' : 'Chấp nhận'}
                        </button>
                        <button
                          className={styles.declineBtn}
                          disabled={loadingInvitationId === n.id}
                          onClick={(e) => { e.stopPropagation(); void handleRejectInvitation(n); }}
                        >
                          {loadingInvitationId === n.id ? '...' : 'Từ chối'}
                        </button>
                      </>
                    )}
                  </div>
                )}
              </div>
            </div>
          ))
        )}
      </div>
      
      {notifications.length > 0 && (
        <div className={styles.footer}>
          <button className={styles.viewAllBtn} onClick={onClose}>
            Đóng
          </button>
        </div>
      )}
    </div>
  );
};

export default NotificationDropdown;
