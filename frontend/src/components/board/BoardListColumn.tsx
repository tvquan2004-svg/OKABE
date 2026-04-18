import { useState } from 'react';
import type { TaskList } from '../../services/boardApi';
import styles from '../../pages/BoardPage.module.css';

interface BoardListColumnProps {
  list: TaskList;
  onEditList: (list: TaskList) => void;
  onDeleteList: (listId: number) => void;
  onDeleteCard: (cardId: number) => void;
  onAddCard: (listId: number, title: string) => Promise<void>;
  priorityColor: (priority: string) => string;
}

function BoardListColumn({
  list,
  onEditList,
  onDeleteList,
  onDeleteCard,
  onAddCard,
  priorityColor,
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

  return (
    <div className={styles.column}>
      <div className={styles.columnHeader}>
        <h3>{list.name}</h3>
        <span className={styles.cardCount}>{list.cards.length}</span>
        <button
          className={styles.secondaryActionBtn}
          onClick={() => onEditList(list)}
          title="Edit list"
        >
          Edit
        </button>
        <button
          className={styles.deleteListBtn}
          onClick={() => onDeleteList(list.id)}
          title="Delete list"
        >
          x
        </button>
      </div>

      <div className={styles.cardList}>
        {list.cards.map((card) => (
          <div key={card.id} className={styles.card}>
            <div className={styles.cardHeader}>
              <span
                className={styles.priorityDot}
                style={{ background: priorityColor(card.priority) }}
                title={card.priority}
              />
              <button
                className={styles.deleteCardBtn}
                onClick={() => onDeleteCard(card.id)}
              >
                x
              </button>
            </div>
            <h4 className={styles.cardTitle}>{card.title}</h4>
            {card.description ? (
              <p className={styles.cardDesc}>{card.description}</p>
            ) : null}
            {card.dueDate ? (
              <span className={styles.dueDate}>
                {new Date(card.dueDate).toLocaleDateString()}
              </span>
            ) : null}
          </div>
        ))}

        {isAddingCard ? (
          <div className={styles.addCardForm}>
            <textarea
              value={newCardTitle}
              onChange={(event) => setNewCardTitle(event.target.value)}
              placeholder="Enter card title..."
              className={styles.addCardInput}
              autoFocus
              rows={2}
              onKeyDown={(event) => {
                if (event.key === 'Enter' && !event.shiftKey) {
                  event.preventDefault();
                  void handleAddCard();
                }
              }}
            />
            <div className={styles.addCardActions}>
              <button className="btn btn-primary" onClick={() => void handleAddCard()}>
                Add
              </button>
              <button className="btn btn-outline" onClick={() => setIsAddingCard(false)}>
                Cancel
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
            + Add a card
          </button>
        )}
      </div>
    </div>
  );
}

export default BoardListColumn;
