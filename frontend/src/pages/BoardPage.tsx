import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import BoardListColumn from '../components/board/BoardListColumn';
import EntityModal from '../components/common/EntityModal';
import CardDetailModal from '../components/board/CardDetailModal';
import {
  type TaskList,
  type CardItem,
  useCreateCardMutation,
  useCreateListMutation,
  useDeleteCardMutation,
  useDeleteListMutation,
  useGetBoardQuery,
  useUpdateBoardMutation,
  useUpdateListMutation,
  useSearchCardsQuery,
  type CardSearchParams,
} from '../services/boardApi';
import { BoardFilter } from '../components/board/BoardFilter';
import { useGetWorkspaceQuery, useGetWorkspaceMembersQuery } from '../services/workspaceApi';
import styles from './BoardPage.module.css';

function BoardPage() {
  const { boardId } = useParams<{ boardId: string }>();
  const navigate = useNavigate();
  const id = Number(boardId);

  const { data: boardData, isLoading } = useGetBoardQuery(id);
  const [createList] = useCreateListMutation();
  const [updateList, { isLoading: isUpdatingList }] = useUpdateListMutation();
  const [updateBoard, { isLoading: isUpdatingBoard }] = useUpdateBoardMutation();
  const [createCard] = useCreateCardMutation();
  const [deleteList] = useDeleteListMutation();
  const [deleteCard] = useDeleteCardMutation();

  const board = boardData?.data;
  const lists = board?.lists ?? [];

  const { data: workspaceData } = useGetWorkspaceQuery(board?.workspaceId ?? 0, {
    skip: !board?.workspaceId,
  });
  const canManageBoard =
    workspaceData?.data.currentUserRole === 'OWNER' ||
    workspaceData?.data.currentUserRole === 'ADMIN';

  const { data: membersData } = useGetWorkspaceMembersQuery(board?.workspaceId ?? 0, {
    skip: !board?.workspaceId,
  });

  const [newListName, setNewListName] = useState('');
  const [showAddList, setShowAddList] = useState(false);
  const [boardName, setBoardName] = useState('');
  const [boardDescription, setBoardDescription] = useState('');
  const [isEditBoardModalOpen, setIsEditBoardModalOpen] = useState(false);
  const [editingList, setEditingList] = useState<TaskList | null>(null);
  const [listName, setListName] = useState('');
  
  // Phase 2: Card Detail Modal State
  const [selectedCard, setSelectedCard] = useState<CardItem | null>(null);

  // Phase 2: Search & Filter State
  const [filters, setFilters] = useState<CardSearchParams>({});
  const { data: searchData } = useSearchCardsQuery(
    { boardId: id, params: filters },
    { skip: !id || Object.keys(filters).length === 0 }
  );

  const matchedCardIds = searchData?.data?.content.map(c => c.id) ?? null;
  const isFiltering = Object.keys(filters).some(key => {
    const val = (filters as any)[key];
    return val !== undefined && val !== '' && (Array.isArray(val) ? val.length > 0 : true);
  });

  const openBoardEditModal = () => {
    if (!board) return;
    setBoardName(board.name);
    setBoardDescription(board.description ?? '');
    setIsEditBoardModalOpen(true);
  };

  const handleAddList = async () => {
    if (!newListName.trim() || !board) return;
    await createList({ boardId: id, name: newListName.trim() }).unwrap();
    setNewListName('');
    setShowAddList(false);
  };

  const handleSaveBoard = async () => {
    if (!board || !boardName.trim()) return;
    await updateBoard({
      id,
      body: {
        name: boardName.trim(),
        description: boardDescription.trim() || null,
      },
    }).unwrap();
    setIsEditBoardModalOpen(false);
  };

  const handleOpenEditList = (list: TaskList) => {
    setEditingList(list);
    setListName(list.name);
  };

  const handleSaveList = async () => {
    if (!editingList || !listName.trim()) return;
    await updateList({
      id: editingList.id,
      boardId: id,
      body: { name: listName.trim() },
    }).unwrap();
    setEditingList(null);
  };

  const handleAddCard = async (listId: number, title: string) => {
    await createCard({ listId, boardId: id, title: title.trim() }).unwrap();
  };

  const handleDeleteList = async (listId: number) => {
    if (confirm('Delete this list and all its cards?')) {
      await deleteList({ id: listId, boardId: id }).unwrap();
    }
  };

  const handleDeleteCard = async (cardId: number) => {
    await deleteCard({ id: cardId, boardId: id }).unwrap();
  };

  const priorityColor = (priority: string) => {
    switch (priority) {
      case 'CRITICAL': return '#ef4444';
      case 'HIGH': return '#f59e0b';
      case 'MEDIUM': return '#3b82f6';
      case 'LOW': return '#22c55e';
      default: return '#64748b';
    }
  };

  if (isLoading) return <div className={styles.loading}>Loading board...</div>;
  if (!board) return <div className={styles.loading}>Board not found</div>;

  // Update selectedCard if board data changes (to keep modal in sync)
  const currentCard = selectedCard 
    ? lists.flatMap(l => l.cards).find(c => c.id === selectedCard.id) 
    : null;

  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <button className={styles.backBtn} onClick={() => navigate(-1)}>Back</button>
        <div className={styles.boardMeta}>
          <h1 className={styles.boardName}>{board.name}</h1>
          {board.description ? <span className={styles.boardDesc}>{board.description}</span> : null}
        </div>
        {canManageBoard ? (
          <button className="btn btn-outline" onClick={openBoardEditModal}>Edit board</button>
        ) : null}
      </header>

      <BoardFilter 
        labels={board.lists?.flatMap(l => l.cards).flatMap(c => c.labels).reduce((acc: any[], curr) => acc.find(x => x.id === curr.id) ? acc : [...acc, curr], []) ?? []}
        members={membersData?.data.map(m => ({
          id: m.userId,
          username: m.username,
          email: m.email,
          avatarUrl: m.avatarUrl
        })) ?? []}
        onFilterChange={setFilters}
      />

      <div className={styles.kanban}>
        {lists.map((list) => (
          <BoardListColumn
            key={list.id}
            list={list}
            onEditList={handleOpenEditList}
            onDeleteList={(listId) => void handleDeleteList(listId)}
            onDeleteCard={(cardId) => void handleDeleteCard(cardId)}
             onAddCard={handleAddCard}
            onCardClick={setSelectedCard}
            priorityColor={priorityColor}
            matchedCardIds={isFiltering ? matchedCardIds : null}
          />
        ))}

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
                onKeyDown={(e) => e.key === 'Enter' && void handleAddList()}
              />
              <div className={styles.addCardActions}>
                <button className="btn btn-primary" onClick={() => void handleAddList()}>Add</button>
                <button className="btn btn-outline" onClick={() => setShowAddList(false)}>Cancel</button>
              </div>
            </div>
          ) : (
            <button className={styles.addListBtn} onClick={() => setShowAddList(true)}>+ Add another list</button>
          )}
        </div>
      </div>

      {isEditBoardModalOpen ? (
        <EntityModal
          title="Edit board"
          nameLabel="Board name"
          nameValue={boardName}
          namePlaceholder="Board name"
          descriptionValue={boardDescription}
          onNameChange={setBoardName}
          onDescriptionChange={setBoardDescription}
          onClose={() => setIsEditBoardModalOpen(false)}
          onSubmit={() => void handleSaveBoard()}
          submitLabel="Save changes"
          isSubmitting={isUpdatingBoard}
        />
      ) : null}

      {editingList ? (
        <EntityModal
          title="Edit list"
          nameLabel="List name"
          nameValue={listName}
          namePlaceholder="List name"
          onNameChange={setListName}
          onClose={() => setEditingList(null)}
          onSubmit={() => void handleSaveList()}
          submitLabel="Save changes"
          isSubmitting={isUpdatingList}
          showDescription={false}
        />
      ) : null}

      {selectedCard && currentCard ? (
        <CardDetailModal
          card={currentCard}
          boardId={id}
          workspaceId={board.workspaceId}
          onClose={() => setSelectedCard(null)}
          priorityColor={priorityColor}
        />
      ) : null}
    </div>
  );
}

export default BoardPage;
