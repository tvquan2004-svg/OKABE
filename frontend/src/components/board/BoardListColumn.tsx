import { useState } from 'react';
import type { TaskList, CardItem } from '../../services/boardApi';
import styles from '../../pages/BoardPage.module.css';
import { useDroppable } from '@dnd-kit/core';
import {
  SortableContext,
  verticalListSortingStrategy,
} from '@dnd-kit/sortable';
import SortableCard from './SortableCard';

interface BoardListColumnProps {
  list: TaskList;
  onEditList: (list: TaskList) => void;
  onDeleteList: (listId: number) => void;
  onAddCard: (listId: number, title: string) => Promise<void>;
  onCardClick: (card: CardItem) => void;
  priorityColor: (priority: string) => string;
  matchedCardIds: number[] | null;
}

function BoardListColumn({
  list,
  onEditList,
  onDeleteList,
  onAddCard,
  onCardClick,
  priorityColor,
  matchedCardIds,
}: BoardListColumnProps) {
  const [isAddingCard, setIsAddingCard] = useState(false);
  const [newCardTitle, setNewCardTitle] = useState('');

  const handleAddCard = async () => {
    if (!newCardTitle.trim()) {
      return;
    }

    await onAddCard(list.id, newCardTitle);
    setNewCardTitle('');
    setIsAddingCard(false);
  };

  const { setNodeRef } = useDroppable({
    id: `list-${list.id}`,
  });

  const cardIds = list.cards.map(c => c.id);

  return (
    <div className={styles.column} ref={setNodeRef}>
      <div className={styles.columnHeader}>
        <h3>{list.name}</h3>
        <span className={styles.cardCount}>{list.cards.length}</span>
        <div className={styles.addActions}>
          <button
            className={styles.secondaryActionBtn}
            onClick={() => onEditList(list)}
            title="Sửa danh sách"
          >
            Sửa
          </button>
          <button
            className={styles.deleteBtn}
            onClick={() => onDeleteList(list.id)}
            title="Xóa danh sách"
          >
            <span style={{ fontSize: '1.2rem' }}>&times;</span>
          </button>
        </div>
      </div>

      <div className={styles.cardList}>
        <SortableContext items={cardIds} strategy={verticalListSortingStrategy}>
          {list.cards.map((card) => (
            <SortableCard
              key={card.id}
              card={card}
              onCardClick={onCardClick}
              priorityColor={priorityColor}
              matchedCardIds={matchedCardIds}
            />
          ))}
        </SortableContext>

        {isAddingCard ? (
          <div className={styles.addForm}>
            <textarea
              value={newCardTitle}
              onChange={(event) => setNewCardTitle(event.target.value)}
              placeholder="Nhập tiêu đề thẻ..."
              className={styles.addInput}
              autoFocus
              rows={2}
              onKeyDown={(event) => {
                if (event.key === 'Enter' && !event.shiftKey) {
                  event.preventDefault();
                  void handleAddCard();
                }
              }}
            />
            <div className={styles.addActions}>
              <button className="btn btn-primary btn-sm" onClick={() => void handleAddCard()}>
                Thêm
              </button>
              <button className="btn btn-outline btn-sm" onClick={() => setIsAddingCard(false)}>
                Hủy
              </button>
            </div>
          </div>
        ) : (
          <button
            className={styles.addCardBtn}
            onClick={() => {
              setIsAddingCard(true);
              setNewCardTitle('');
            }}
          >
            + Thêm thẻ mới
          </button>
        )}
      </div>
    </div>
  );
}

export default BoardListColumn;
