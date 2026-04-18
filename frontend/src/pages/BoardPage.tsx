import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  useGetBoardQuery,
  useCreateListMutation,
  useCreateCardMutation,
  useDeleteListMutation,
  useDeleteCardMutation,
} from '../services/boardApi';
import styles from './BoardPage.module.css';

function BoardPage() {
  const { boardId } = useParams<{ boardId: string }>();
  const navigate = useNavigate();
  const id = Number(boardId);

  const { data: boardData, isLoading } = useGetBoardQuery(id);
  const [createList] = useCreateListMutation();
  const [createCard] = useCreateCardMutation();
  const [deleteList] = useDeleteListMutation();
  const [deleteCard] = useDeleteCardMutation();

  const [newListName, setNewListName] = useState('');
  const [showAddList, setShowAddList] = useState(false);
  const [addingCardToList, setAddingCardToList] = useState<number | null>(null);
  const [newCardTitle, setNewCardTitle] = useState('');

  const board = boardData?.data;
  const lists = board?.lists ?? [];

  const handleAddList = async () => {
    if (!newListName.trim() || !board) return;
    await createList({ boardId: id, name: newListName }).unwrap();
    setNewListName('');
    setShowAddList(false);
  };

  const handleAddCard = async (listId: number) => {
    if (!newCardTitle.trim() || !board) return;
    await createCard({ listId, boardId: id, title: newCardTitle }).unwrap();
    setNewCardTitle('');
    setAddingCardToList(null);
  };

  const handleDeleteList = async (listId: number) => {
    if (confirm('Delete this list and all its cards?')) {
      await deleteList({ id: listId, boardId: id }).unwrap();
    }
  };

  const handleDeleteCard = async (cardId: number) => {
    await deleteCard({ id: cardId, boardId: id }).unwrap();
  };

  const priorityColor = (p: string) => {
    switch (p) {
      case 'CRITICAL': return '#ef4444';
      case 'HIGH': return '#f59e0b';
      case 'MEDIUM': return '#3b82f6';
      case 'LOW': return '#22c55e';
      default: return '#64748b';
    }
  };

  if (isLoading) {
    return <div className={styles.loading}>Loading board...</div>;
  }

  if (!board) {
    return <div className={styles.loading}>Board not found</div>;
  }

  return (
    <div className={styles.container}>
      {/* Header */}
      <header className={styles.header}>
        <button className={styles.backBtn} onClick={() => navigate(-1)}>← Back</button>
        <h1 className={styles.boardName}>{board.name}</h1>
        {board.description && <span className={styles.boardDesc}>{board.description}</span>}
      </header>

      {/* Kanban Board */}
      <div className={styles.kanban}>
        {lists.map((list) => (
          <div key={list.id} className={styles.column}>
            <div className={styles.columnHeader}>
              <h3>{list.name}</h3>
              <span className={styles.cardCount}>{list.cards.length}</span>
              <button
                className={styles.deleteListBtn}
                onClick={() => handleDeleteList(list.id)}
                title="Delete list"
              >×</button>
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
                      onClick={() => handleDeleteCard(card.id)}
                    >×</button>
                  </div>
                  <h4 className={styles.cardTitle}>{card.title}</h4>
                  {card.description && (
                    <p className={styles.cardDesc}>{card.description}</p>
                  )}
                  {card.dueDate && (
                    <span className={styles.dueDate}>
                      📅 {new Date(card.dueDate).toLocaleDateString()}
                    </span>
                  )}
                </div>
              ))}

              {/* Add Card */}
              {addingCardToList === list.id ? (
                <div className={styles.addCardForm}>
                  <textarea
                    value={newCardTitle}
                    onChange={(e) => setNewCardTitle(e.target.value)}
                    placeholder="Enter card title..."
                    className={styles.addCardInput}
                    autoFocus
                    rows={2}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter' && !e.shiftKey) {
                        e.preventDefault();
                        handleAddCard(list.id);
                      }
                    }}
                  />
                  <div className={styles.addCardActions}>
                    <button className="btn btn-primary" onClick={() => handleAddCard(list.id)}>
                      Add
                    </button>
                    <button className="btn btn-outline" onClick={() => setAddingCardToList(null)}>
                      Cancel
                    </button>
                  </div>
                </div>
              ) : (
                <button
                  className={styles.addCardBtn}
                  onClick={() => { setAddingCardToList(list.id); setNewCardTitle(''); }}
                >
                  + Add a card
                </button>
              )}
            </div>
          </div>
        ))}

        {/* Add List */}
        <div className={styles.addListColumn}>
          {showAddList ? (
            <div className={styles.addListForm}>
              <input
                type="text"
                value={newListName}
                onChange={(e) => setNewListName(e.target.value)}
                placeholder="Enter list name..."
                className={styles.addListInput}
                autoFocus
                onKeyDown={(e) => e.key === 'Enter' && handleAddList()}
              />
              <div className={styles.addCardActions}>
                <button className="btn btn-primary" onClick={handleAddList}>Add</button>
                <button className="btn btn-outline" onClick={() => setShowAddList(false)}>Cancel</button>
              </div>
            </div>
          ) : (
            <button className={styles.addListBtn} onClick={() => setShowAddList(true)}>
              + Add another list
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

export default BoardPage;
