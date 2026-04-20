import React from 'react';
import { useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import type { CardItem } from '../../services/boardApi';
import styles from '../../pages/BoardPage.module.css';

interface SortableCardProps {
  card: CardItem;
  onDeleteCard: (cardId: number) => void;
  onCardClick: (card: CardItem) => void;
  priorityColor: (priority: string) => string;
  matchedCardIds: number[] | null;
}

const SortableCard: React.FC<SortableCardProps> = ({
  card,
  onDeleteCard,
  onCardClick,
  priorityColor,
  matchedCardIds,
}) => {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: card.id });

  const style = {
    transform: CSS.Translate.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
    zIndex: isDragging ? 1000 : 1,
  };

  const getFullUrl = (url?: string) => {
    if (!url) return '';
    if (url.startsWith('http')) return url;
    return `http://localhost:8080${url}`;
  };

  const coverImage = card.attachments?.find(a => a.mimeType.includes('image'))?.url;
  const totalItems = card.checklists?.reduce((acc, c) => acc + c.items.length, 0) || 0;
  const completedItems = card.checklists?.reduce((acc, c) => acc + c.items.filter((i) => i.isCompleted).length, 0) || 0;
  const isChecklistCompleted = totalItems > 0 && totalItems === completedItems;

  return (
    <div
      ref={setNodeRef}
      style={style}
      {...attributes}
      {...listeners}
      className={`${styles.card} ${
        matchedCardIds !== null && !matchedCardIds.includes(card.id) ? styles.dimmed : ''
      }`}
      onClick={() => onCardClick(card)}
    >
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
                style={{ width: '32px', height: '6px', background: l.color, borderRadius: '3px' }}
              />
            ))}
          </div>
          <span
            className={styles.priorityDot}
            style={{ background: priorityColor(card.priority) }}
            title={card.priority}
          />
          <button
            className={styles.deleteCardBtn}
            onClick={(e) => {
              e.stopPropagation();
              onDeleteCard(card.id);
            }}
            onPointerDown={(e) => e.stopPropagation()}
          >
            x
          </button>
        </div>

        <h4 className={styles.cardTitle}>{card.title}</h4>

        <div style={{ display: 'flex', gap: '8px', alignItems: 'center', marginTop: '8px' }}>
          {card.description && (
            <span title="Có mô tả" style={{ fontSize: '0.8rem' }}>📝</span>
          )}
          
          {totalItems > 0 && (
            <span
              title="Tiến trình"
              style={{
                fontSize: '0.75rem',
                background: isChecklistCompleted ? '#10b981' : 'rgba(255,255,255,0.1)',
                color: isChecklistCompleted ? 'white' : '#94a3b8',
                padding: '2px 6px',
                borderRadius: '3px',
                display: 'flex',
                alignItems: 'center',
                gap: '4px'
              }}
            >
              ✅ {completedItems}/{totalItems}
            </span>
          )}

          {card.attachments?.length > 0 && (
            <span
              title="Đính kèm"
              style={{
                fontSize: '0.75rem',
                background: 'rgba(255,255,255,0.1)',
                color: '#94a3b8',
                padding: '2px 6px',
                borderRadius: '3px',
              }}
            >
              📎 {card.attachments.length}
            </span>
          )}

          <div style={{ display: 'flex', marginLeft: 'auto' }}>
            {card.members?.slice(0, 3).map((member, i) => (
              <div
                key={member.id}
                style={{
                  width: '28px',
                  height: '28px',
                  borderRadius: '50%',
                  background: '#334155',
                  color: '#f1f5f9',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: '0.7rem',
                  fontWeight: 700,
                  border: '2px solid #1e293b',
                  marginLeft: i > 0 ? '-10px' : '0',
                  zIndex: 3 - i,
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
                  width: '28px',
                  height: '28px',
                  borderRadius: '50%',
                  background: '#475569',
                  color: '#f1f5f9',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: '0.6rem',
                  fontWeight: 700,
                  border: '2px solid #1e293b',
                  marginLeft: '-10px',
                  zIndex: 0,
                }}
              >
                +{card.members.length - 3}
              </div>
            )}
          </div>
        </div>

        {card.dueDate && (
          <div style={{ marginTop: '8px', fontSize: '0.7rem', color: '#94a3b8' }}>
            📅 {new Date(card.dueDate).toLocaleDateString()}
          </div>
        )}
      </div>
    </div>
  );
};

export default SortableCard;
