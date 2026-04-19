import { useState } from 'react';
import type { TaskList, CardItem } from '../../services/boardApi';
import styles from '../../pages/BoardPage.module.css';

interface BoardListColumnProps {
  list: TaskList;
  onEditList: (list: TaskList) => void;
  onDeleteList: (listId: number) => void;
  onDeleteCard: (cardId: number) => void;
  onAddCard: (listId: number, title: string) => Promise<void>;
  onCardClick: (card: CardItem) => void;
  priorityColor: (priority: string) => string;
}

function BoardListColumn({
  list,
  onEditList,
  onDeleteList,
  onDeleteCard,
  onAddCard,
  onCardClick,
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
        <div style={{ display: 'flex', gap: '4px' }}>
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
      </div>

      <div className={styles.cardList}>
        {list.cards.map((card) => (
          <div key={card.id} className={styles.card} onClick={() => onCardClick(card)}>
            <div className={styles.cardHeader}>
              <div style={{ display: 'flex', gap: '4px', flexWrap: 'wrap', flex: 1 }}>
                {card.labels?.map(l => (
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
              >
                x
              </button>
            </div>
            <h4 className={styles.cardTitle}>{card.title}</h4>
            
            <div style={{ display: 'flex', gap: '8px', alignItems: 'center', marginTop: '8px' }}>
              {card.description ? (
                <span title="This card has a description" style={{ fontSize: '0.8rem' }}>📝</span>
              ) : null}
              {card.checklists?.length > 0 ? (
                <span title="Checklists" style={{ fontSize: '0.8rem', background: '#f1f5f9', padding: '2px 4px', borderRadius: '4px' }}>
                  ✅ {card.checklists.reduce((acc, c) => acc + c.items.filter(i => i.isCompleted).length, 0)}/{card.checklists.reduce((acc, c) => acc + c.items.length, 0)}
                </span>
              ) : null}
              {card.attachments?.length > 0 ? (
                <span title="Attachments" style={{ fontSize: '0.8rem', background: '#f1f5f9', padding: '2px 4px', borderRadius: '4px' }}>
                  📎 {card.attachments.length}
                </span>
              ) : null}
              
              <div style={{ display: 'flex', marginLeft: 'auto' }}>
                {card.members?.slice(0, 3).map((member, i) => (
                  <div 
                    key={member.id} 
                    style={{ 
                      width: '24px', 
                      height: '24px', 
                      borderRadius: '50%', 
                      background: '#e2e8f0', 
                      display: 'flex', 
                      alignItems: 'center', 
                      justifyContent: 'center',
                      fontSize: '0.65rem',
                      fontWeight: 700,
                      border: '2px solid #ffffff',
                      marginLeft: i > 0 ? '-8px' : '0',
                      zIndex: 3 - i,
                      overflow: 'hidden'
                    }}
                    title={member.username}
                  >
                    {member.avatarUrl ? (
                      <img src={member.avatarUrl} alt={member.username} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                    ) : member.username.charAt(0).toUpperCase()}
                  </div>
                ))}
                {card.members?.length > 3 && (
                  <div style={{ 
                    width: '24px', 
                    height: '24px', 
                    borderRadius: '50%', 
                    background: '#f1f5f9', 
                    display: 'flex', 
                    alignItems: 'center', 
                    justifyContent: 'center',
                    fontSize: '0.6rem',
                    fontWeight: 700,
                    border: '2px solid #ffffff',
                    marginLeft: '-8px',
                    zIndex: 0
                  }}>
                    +{card.members.length - 3}
                  </div>
                )}
              </div>

              {card.dueDate ? (
                <span className={styles.dueDate} style={{ fontSize: '0.7rem' }}>
                  {new Date(card.dueDate).toLocaleDateString()}
                </span>
              ) : null}
            </div>
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
