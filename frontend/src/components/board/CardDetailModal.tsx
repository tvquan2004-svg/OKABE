import React, { useState, useEffect } from 'react';
import {
  CardItem,
  useUpdateCardMutation,
  useCreateChecklistMutation,
  useCreateChecklistItemMutation,
  useUpdateChecklistItemMutation,
  useCreateLabelMutation,
  useAddLabelToCardMutation,
  useRemoveLabelFromCardMutation,
  useGetBoardLabelsQuery,
} from '../../services/boardApi';
import styles from './CardDetailModal.module.css';

interface CardDetailModalProps {
  card: CardItem;
  boardId: number;
  onClose: () => void;
  priorityColor: (priority: string) => string;
}

const PRESET_COLORS = [
  '#22c55e', '#3b82f6', '#f59e0b', '#ef4444', '#a855f7', 
  '#ec4899', '#06b6d4', '#64748b'
];

const CardDetailModal: React.FC<CardDetailModalProps> = ({
  card,
  boardId,
  onClose,
  priorityColor,
}) => {
  const [title, setTitle] = useState(card.title);
  const [description, setDescription] = useState(card.description || '');
  const [newItemContent, setNewItemContent] = useState<{ [key: number]: string }>({});

  const [updateCard] = useUpdateCardMutation();
  const [createChecklist] = useCreateChecklistMutation();
  const [createChecklistItem] = useCreateChecklistItemMutation();
  const [updateChecklistItem] = useUpdateChecklistItemMutation();
  const [createLabel] = useCreateLabelMutation();
  const [addLabelToCard] = useAddLabelToCardMutation();
  const [removeLabelFromCard] = useRemoveLabelFromCardMutation();
  
  const { data: labelsData } = useGetBoardLabelsQuery(boardId);
  const boardLabels = labelsData?.data || [];

  useEffect(() => {
    setTitle(card.title);
    setDescription(card.description || '');
  }, [card]);

  const handleUpdateCard = async (body: Partial<CardItem>) => {
    await updateCard({ id: card.id, boardId, body }).unwrap();
  };

  const handleCreateChecklist = async () => {
    const name = prompt('Enter checklist name:');
    if (name?.trim()) {
      await createChecklist({ cardId: card.id, boardId, name: name.trim() }).unwrap();
    }
  };

  const handleAddItem = async (checklistId: number) => {
    const content = newItemContent[checklistId];
    if (content?.trim()) {
      await createChecklistItem({ checklistId, boardId, content: content.trim() }).unwrap();
      setNewItemContent({ ...newItemContent, [checklistId]: '' });
    }
  };

  const handleToggleItem = async (itemId: number, isCompleted: boolean) => {
    await updateChecklistItem({ itemId, boardId, body: { isCompleted } }).unwrap();
  };

  const handleAddLabel = async (labelId: number) => {
    if (!card.labels.some(l => l.id === labelId)) {
      await addLabelToCard({ cardId: card.id, labelId, boardId }).unwrap();
    }
  };

  const handleRemoveLabel = async (labelId: number) => {
    await removeLabelFromCard({ cardId: card.id, labelId, boardId }).unwrap();
  };

  const handleCreateAndAddLabel = async (color: string) => {
    const name = prompt('Enter label name (optional):');
    const res = await createLabel({ boardId, color, name: name || '' }).unwrap();
    if (res.success) {
      await addLabelToCard({ cardId: card.id, labelId: res.data.id, boardId }).unwrap();
    }
  };

  return (
    <div className={styles.overlay} onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className={styles.modal}>
        <header className={styles.header}>
          <div className={styles.titleWrapper}>
            <input
              className={styles.titleInput}
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              onBlur={() => title !== card.title && handleUpdateCard({ title })}
            />
            <div style={{ fontSize: '0.85rem', color: '#64748b', marginTop: '0.25rem' }}>
              in list <span style={{ textDecoration: 'underline' }}>TaskList</span>
            </div>
          </div>
          <button className={styles.closeBtn} onClick={onClose}>&times;</button>
        </header>

        <div className={styles.body}>
          <main className={styles.mainContent}>
            {/* Labels Section */}
            {card.labels.length > 0 && (
              <div className={styles.section}>
                <h3 className={styles.sidebarLabel}>Labels</h3>
                <div className={styles.labelsList}>
                  {card.labels.map(label => (
                    <div
                      key={label.id}
                      className={styles.labelItem}
                      style={{ background: label.color }}
                      onClick={() => handleRemoveLabel(label.id)}
                      title="Click to remove"
                    >
                      {label.name}
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Description Section */}
            <div className={styles.section}>
              <h3 className={styles.sectionTitle}>
                <span>📝</span> Description
              </h3>
              <textarea
                className={styles.descriptionBox}
                placeholder="Add a more detailed description..."
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                onBlur={() => description !== (card.description || '') && handleUpdateCard({ description })}
              />
            </div>

            {/* Checklists Section */}
            {card.checklists.map(checklist => {
              const completedCount = checklist.items.filter(i => i.isCompleted).length;
              const percent = checklist.items.length > 0 
                ? Math.round((completedCount / checklist.items.length) * 100) 
                : 0;
              
              return (
                <div key={checklist.id} className={styles.checklist}>
                  <div className={styles.checklistHeader}>
                    <h3 className={styles.sectionTitle}>
                      <span>✅</span> {checklist.name}
                    </h3>
                  </div>
                  
                  <div className={styles.progressBar}>
                    <span className={styles.progressPercent}>{percent}%</span>
                    <div className={styles.progressTrack}>
                      <div className={styles.progressFill} style={{ width: `${percent}%` }} />
                    </div>
                  </div>

                  <div className={styles.itemsList}>
                    {checklist.items.map(item => (
                      <div key={item.id} className={styles.checklistItem}>
                        <input
                          type="checkbox"
                          className={styles.checkbox}
                          checked={item.isCompleted}
                          onChange={(e) => handleToggleItem(item.id, e.target.checked)}
                        />
                        <span className={`${styles.itemContent} ${item.isCompleted ? styles.itemCompleted : ''}`}>
                          {item.content}
                        </span>
                      </div>
                    ))}
                    <input
                      className={styles.addItemInput}
                      placeholder="Add an item..."
                      value={newItemContent[checklist.id] || ''}
                      onChange={(e) => setNewItemContent({ ...newItemContent, [checklist.id]: e.target.value })}
                      onKeyDown={(e) => e.key === 'Enter' && handleAddItem(checklist.id)}
                    />
                  </div>
                </div>
              );
            })}
          </main>

          <aside className={styles.sidebar}>
            <div className={styles.sidebarGroup}>
              <h3 className={styles.sidebarLabel}>Add to card</h3>
              <button className={styles.actionBtn} onClick={handleCreateChecklist}>
                <span>✅</span> Checklist
              </button>
            </div>

            <div className={styles.sidebarGroup}>
              <h3 className={styles.sidebarLabel}>Labels (Quick Add)</h3>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '4px' }}>
                {PRESET_COLORS.map(color => (
                  <div
                    key={color}
                    style={{ width: '32px', height: '24px', background: color, borderRadius: '4px', cursor: 'pointer' }}
                    onClick={() => handleCreateAndAddLabel(color)}
                  />
                ))}
              </div>
              {boardLabels.length > 0 && (
                <div style={{ marginTop: '8px' }}>
                   <h3 className={styles.sidebarLabel} style={{ fontSize: '0.65rem' }}>Board Labels</h3>
                   <div className={styles.labelsList}>
                    {boardLabels.map(l => (
                      <div 
                        key={l.id} 
                        style={{ width: '100%', height: '8px', background: l.color, borderRadius: '2px', cursor: 'pointer' }}
                        onClick={() => handleAddLabel(l.id)}
                        title={l.name}
                      />
                    ))}
                   </div>
                </div>
              )}
            </div>

            <div className={styles.sidebarGroup}>
              <h3 className={styles.sidebarLabel}>Priority</h3>
              <select
                className={styles.prioritySelect}
                value={card.priority}
                onChange={(e) => handleUpdateCard({ priority: e.target.value })}
                style={{ borderLeft: `4px solid ${priorityColor(card.priority)}` }}
              >
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="CRITICAL">Critical</option>
              </select>
            </div>

            <div className={styles.sidebarGroup}>
              <h3 className={styles.sidebarLabel}>Due Date</h3>
              <input
                type="datetime-local"
                className={styles.datePicker}
                value={card.dueDate ? card.dueDate.slice(0, 16) : ''}
                onChange={(e) => handleUpdateCard({ dueDate: e.target.value })}
              />
            </div>
          </aside>
        </div>
      </div>
    </div>
  );
};

export default CardDetailModal;
