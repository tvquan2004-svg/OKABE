import React from 'react';
import { FiX, FiActivity, FiUser, FiClock, FiCheckCircle, FiPlusCircle, FiArrowRight, FiFileText } from 'react-icons/fi';
import { useGetBoardActivitiesQuery } from '../../services/boardApi';
import styles from './BoardActivitySidebar.module.css';

interface BoardActivitySidebarProps {
  boardId: number;
  isOpen: boolean;
  onClose: () => void;
  onCardClick: (cardId: number) => void;
}

const formatRelativeTime = (dateString: string) => {
  const date = new Date(dateString);
  const now = new Date();
  const diffInSeconds = Math.floor((now.getTime() - date.getTime()) / 1000);

  if (diffInSeconds < 60) return 'Vừa xong';
  
  const diffInMinutes = Math.floor(diffInSeconds / 60);
  if (diffInMinutes < 60) return `${diffInMinutes} phút trước`;
  
  const diffInHours = Math.floor(diffInMinutes / 60);
  if (diffInHours < 24) return `${diffInHours} giờ trước`;
  
  const diffInDays = Math.floor(diffInHours / 24);
  if (diffInDays < 30) return `${diffInDays} ngày trước`;
  
  return date.toLocaleDateString('vi-VN');
};

const BoardActivitySidebar: React.FC<BoardActivitySidebarProps> = ({ boardId, isOpen, onClose, onCardClick }) => {
  const { data: activitiesData, isLoading } = useGetBoardActivitiesQuery(boardId, {
    skip: !isOpen,
    pollingInterval: 10000,
  });

  const getActionIcon = (type: string) => {
    switch (type) {
      case 'CARD_CREATED': return <FiPlusCircle className={styles.iconPlus} />;
      case 'CARD_MOVED': return <FiArrowRight className={styles.iconMove} />;
      case 'CARD_UPDATED': return <FiFileText className={styles.iconUpdate} />;
      case 'COMMENT_ADDED': return <FiActivity className={styles.iconComment} />;
      default: return <FiCheckCircle className={styles.iconDefault} />;
    }
  };

  return (
    <>
      <div className={`${styles.overlay} ${isOpen ? styles.overlayVisible : ''}`} onClick={onClose} />
      <aside className={`${styles.sidebar} ${isOpen ? styles.sidebarOpen : ''}`}>
        <div className={styles.header}>
          <div className={styles.headerTitle}>
            <FiActivity />
            <span>Hoạt động bảng</span>
          </div>
          <button className={styles.closeBtn} onClick={onClose}>
            <FiX />
          </button>
        </div>

        <div className={styles.content}>
          {isLoading ? (
            <div className={styles.loading}>Đang tải hoạt động...</div>
          ) : activitiesData?.data.length === 0 ? (
            <div className={styles.empty}>Chưa có hoạt động nào</div>
          ) : (
            <div className={styles.timeline}>
              {activitiesData?.data.map((activity) => (
                <div 
                  key={activity.id} 
                  className={`${styles.activityItem} ${activity.cardId ? styles.clickable : ''}`}
                  onClick={() => activity.cardId && onCardClick(activity.cardId)}
                >
                  <div className={styles.avatarWrapper}>
                    {activity.avatarUrl ? (
                      <img src={activity.avatarUrl} alt={activity.username} className={styles.avatar} />
                    ) : (
                      <div className={styles.avatarPlaceholder}>
                        <FiUser />
                      </div>
                    )}
                    <div className={styles.actionBadge}>
                      {getActionIcon(activity.actionType)}
                    </div>
                  </div>
                  <div className={styles.activityBody}>
                    <div className={styles.activityText}>
                      <span className={styles.username}>{activity.username}</span>
                      <p className={styles.description}>{activity.description}</p>
                    </div>
                    <div className={styles.activityMeta}>
                      <FiClock size={12} />
                      <span>{formatRelativeTime(activity.createdAt)}</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </aside>
    </>
  );
};

export default BoardActivitySidebar;
