import React from 'react';
import { useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { FiAlertCircle, FiCalendar, FiArchive, FiTrash2 } from 'react-icons/fi';
import type { CardItem } from '../../services/boardApi';
import styles from '../../pages/BoardPage.module.css';
import { getFullFileUrl } from '../../utils/urlHelper';

interface SortableCardProps {
  card: CardItem;
  onCardClick: (card: CardItem) => void;
  onArchiveCard?: (cardId: number) => Promise<void>;
  onDeleteCard?: (cardId: number) => Promise<void>;
  priorityColor: (priority: string) => string;
  matchedCardIds: number[] | null;
  isDragDisabled?: boolean;
}

const SortableCard: React.FC<SortableCardProps> = ({
  card,
  onCardClick,
  onArchiveCard,
  onDeleteCard,
  priorityColor,
  matchedCardIds,
  isDragDisabled = false,
}) => {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ 
    id: card.id,
    disabled: isDragDisabled
  });

  const style = {
    transform: CSS.Translate.toString(transform),
    transition,
    zIndex: isDragging ? 1000 : 1,
  };

  const getFullUrl = getFullFileUrl;

  const coverImage = card.attachments?.find(a => a.mimeType.includes('image'))?.url;
  const totalItems = card.checklists?.reduce((acc, c) => acc + c.items.length, 0) || 0;
  const completedItems = card.checklists?.reduce((acc, c) => acc + c.items.filter((i) => i.isCompleted).length, 0) || 0;
  const isChecklistCompleted = totalItems > 0 && totalItems === completedItems;

  const isOverdue = card.dueDate && new Date(card.dueDate) < new Date();

  return (
    <div
      ref={setNodeRef}
      style={style}
      {...(!isDragDisabled ? attributes : {})}
      {...(!isDragDisabled ? listeners : {})}
      className={`${styles.card} ${
        isDragging ? styles.isDragging : ''
      } ${
        matchedCardIds !== null && !matchedCardIds.includes(card.id) ? styles.dimmed : ''
      }`}
      onClick={() => onCardClick(card)}
    >
      {!isDragDisabled && (
        <div className={styles.cardActions}>
          <button
            className={styles.cardActionBtn}
            onClick={(e) => { e.stopPropagation(); onArchiveCard?.(card.id); }}
            title="Lưu trữ thẻ"
          >
            <FiArchive size={13} />
          </button>
          <button
            className={styles.cardActionBtn}
            onClick={(e) => {
              e.stopPropagation();
              if (confirm('Xoá thẻ này?')) onDeleteCard?.(card.id);
            }}
            title="Xoá thẻ"
          >
            <FiTrash2 size={13} />
          </button>
        </div>
      )}
      {coverImage && (
        <div className={styles.cardCover}>
          <img src={getFullUrl(coverImage)} alt="Cover" />
        </div>
      )}
      
      <div className={styles.cardContent}>
        <div className={styles.cardHeader}>
          <div style={{ display: 'flex', gap: '4px', flexWrap: 'wrap', flex: 1 }}>
            {card.labels?.map((l) => (
              <div
                key={l.id}
                style={{ width: '24px', height: '4px', background: l.color, borderRadius: '2px', opacity: 0.8 }}
                title={l.name}
              />
            ))}
          </div>
          <div
            className={styles.priorityDot}
            style={{ background: priorityColor(card.priority) }}
            title={`Độ ưu tiên: ${card.priority}`}
          />
        </div>

        <h4 className={styles.cardTitle}>{card.title}</h4>
        
        {card.description && (
          <p className={styles.cardDesc}>{card.description}</p>
        )}

        <div className={styles.cardBadges}>
          {totalItems > 0 && (
            <div className={`${styles.badgeItem} ${isChecklistCompleted ? styles.completed : ''}`}>
              <span>{isChecklistCompleted ? '✓' : '☐'}</span>
              {completedItems}/{totalItems}
            </div>
          )}

          {card.attachments?.length > 0 && (
            <div className={styles.badgeItem}>
              <span>📎</span>
              {card.attachments.length}
            </div>
          )}

          {card.dueDate && (
            <div className={`${styles.badgeItem} ${isOverdue ? styles.overdueBadge : ''}`}>
              {isOverdue ? <FiAlertCircle className={styles.alarmIcon} /> : <FiCalendar />}
              <span>
                {card.startDate && `${new Date(card.startDate).toLocaleDateString('vi-VN', { day: 'numeric', month: 'short' })} - `}
                {new Date(card.dueDate).toLocaleDateString('vi-VN', { day: 'numeric', month: 'short' })}
              </span>
            </div>
          )}

          <div style={{ display: 'flex', marginLeft: 'auto' }}>
            {card.members?.slice(0, 3).map((member, i) => (
              <div
                key={member.id}
                style={{
                  width: '24px',
                  height: '24px',
                  borderRadius: '50%',
                  background: 'var(--color-bg-elevated)',
                  color: 'white',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: '9px',
                  fontWeight: 700,
                  border: '2px solid var(--color-bg-secondary)',
                  marginLeft: i > 0 ? '-8px' : '0',
                  zIndex: 10 - i,
                  overflow: 'hidden',
                }}
                title={member.username}
              >
                {member.avatarUrl ? (
                  <img
                    src={getFullUrl(member.avatarUrl)}
                    alt={member.username}
                    style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                  />
                ) : (
                  member.username.charAt(0).toUpperCase()
                )}
              </div>
            ))}
            {card.members?.length > 3 && (
              <div
                style={{
                  width: '24px',
                  height: '24px',
                  borderRadius: '50%',
                  background: 'var(--color-border)',
                  color: 'white',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: '8px',
                  fontWeight: 700,
                  border: '2px solid var(--color-bg-secondary)',
                  marginLeft: '-8px',
                  zIndex: 0,
                }}
              >
                +{card.members.length - 3}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default SortableCard;
