import React from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  useGetNotificationsQuery, 
  useMarkAsReadMutation, 
  useMarkAllAsReadMutation 
} from '../../services/notificationApi';
import styles from './NotificationDropdown.module.css';

interface NotificationDropdownProps {
  onClose: () => void;
}

const NotificationDropdown: React.FC<NotificationDropdownProps> = ({ onClose }) => {
  const navigate = useNavigate();
  const { data: notificationsRes, isLoading } = useGetNotificationsQuery(
    { page: 0, size: 20 },
    { pollingInterval: 5000 }
  );
  const [markAsRead] = useMarkAsReadMutation();
  const [markAllAsRead] = useMarkAllAsReadMutation();

  const notifications = notificationsRes?.data.content ?? [];

  const handleNotificationClick = async (n: any) => {
    if (!n.isRead) {
      await markAsRead(n.id);
    }
    
    // Navigate based on entity type
    if (n.entityType === 'CARD') {
      // For now just navigate to board if we had boardId, but we only have entityId (cardId)
      // Ideally we'd have a route like /card/:id or include boardId in notification
      // navigate(`/board/${n.boardId}?cardId=${n.entityId}`);
    } else if (n.entityType === 'WORKSPACE') {
      navigate(`/workspace/${n.entityId}`);
    }
    
    onClose();
  };

  const formatTime = (dateStr: string) => {
    const date = new Date(dateStr);
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    
    const minutes = Math.floor(diff / 60000);
    if (minutes < 1) return 'Just now';
    if (minutes < 60) return `${minutes}m ago`;
    
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours}h ago`;
    
    return date.toLocaleDateString();
  };

  return (
    <div className={styles.dropdownContainer} onClick={(e) => e.stopPropagation()}>
      <div className={styles.header}>
        <h3>Notifications</h3>
        {notifications.length > 0 && (
          <button className={styles.markAllBtn} onClick={() => markAllAsRead()}>
            Mark all as read
          </button>
        )}
      </div>

      <div className={styles.list}>
        {isLoading ? (
          <div className={styles.empty}>Loading...</div>
        ) : notifications.length === 0 ? (
          <div className={styles.empty}>
            <span className={styles.emptyIcon}>🔔</span>
            <p>No notifications yet</p>
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
              </div>
            </div>
          ))
        )}
      </div>
      
      {notifications.length > 0 && (
        <div className={styles.footer}>
          <button className={styles.viewAllBtn} onClick={onClose}>
            Close
          </button>
        </div>
      )}
    </div>
  );
};

export default NotificationDropdown;
