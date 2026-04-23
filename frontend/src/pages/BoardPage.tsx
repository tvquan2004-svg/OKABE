import { useState, useEffect } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { useAppDispatch } from '../hooks/useRedux';
import { useWebSocket } from '../hooks/useWebSocket';
import { apiSlice } from '../services/apiSlice';
import BoardListColumn from '../components/board/BoardListColumn';
import EntityModal from '../components/common/EntityModal';
import CardDetailModal from '../components/board/CardDetailModal';
import {
  type TaskList,
  type CardItem,
  useCreateCardMutation,
  useCreateListMutation,
  useDeleteListMutation,
  useGetBoardQuery,
  useUpdateBoardMutation,
  useUpdateListMutation,
  useSearchCardsQuery,
  useMoveCardMutation,
  useArchiveCardMutation,
  type CardSearchParams,
} from '../services/boardApi';
import { BoardFilter } from '../components/board/BoardFilter';
import BackgroundPicker from '../components/board/BackgroundPicker';
import { FiSettings, FiImage, FiCopy, FiArchive, FiCalendar, FiPieChart, FiChevronLeft, FiClock, FiActivity, FiTrash2 } from 'react-icons/fi';
import { useSaveAsTemplateMutation } from '../services/templateApi';
import { 
  useArchiveBoardMutation,
} from '../services/boardApi';
import ArchivedItemsPanel from '../components/board/ArchivedItemsPanel';
import BoardActivitySidebar from '../components/board/BoardActivitySidebar';
import { useGetWorkspaceQuery, useGetWorkspaceMembersQuery } from '../services/workspaceApi';
import styles from './BoardPage.module.css';
import {
  DndContext,
  DragEndEvent,
  PointerSensor,
  useSensor,
  useSensors,
  closestCorners,
  DragOverlay,
  defaultDropAnimationSideEffects,
  useDroppable,
} from '@dnd-kit/core';
import SortableCard from '../components/board/SortableCard';

// Archive Drop Zone Component
const ArchiveDropZone = ({ isDragging }: { isDragging: boolean }) => {
  const { setNodeRef, isOver } = useDroppable({
    id: 'archive-zone',
  });

  if (!isDragging) return null;

  return (
    <div 
      ref={setNodeRef} 
      className={`${styles.archiveZone} ${isOver ? styles.archiveZoneOver : ''}`}
    >
      <div className={styles.archiveZoneContent}>
        <FiTrash2 className={styles.archiveZoneIcon} />
        <span>Kéo vào đây để lưu trữ thẻ</span>
      </div>
    </div>
  );
};

function BoardPage() {
  const { boardId } = useParams<{ boardId: string }>();
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const id = Number(boardId);
  const cardIdFromUrl = searchParams.get('cardId');

  // Phase 3: WebSocket Real-time Updates
  useWebSocket({
    topics: [`/topic/board.${id}`],
    onMessage: (message) => {
      if (message.type.startsWith('CARD_') || message.type.startsWith('LIST_')) {
        dispatch(apiSlice.util.invalidateTags([{ type: 'Board', id }]));
      }
      if (message.type === 'NOTIFICATION_RECEIVED') {
        dispatch(apiSlice.util.invalidateTags(['Notification']));
      }
    },
  });

  const { data: boardData, isLoading } = useGetBoardQuery(id);
  const [createList] = useCreateListMutation();
  const [updateList, { isLoading: isUpdatingList }] = useUpdateListMutation();
  const [updateBoard, { isLoading: isUpdatingBoard }] = useUpdateBoardMutation();
  const [createCard] = useCreateCardMutation();
  const [deleteList] = useDeleteListMutation();
  const [moveCard] = useMoveCardMutation();
  const [archiveCard] = useArchiveCardMutation();
  const [saveAsTemplate, { isLoading: isSavingAsTemplate }] = useSaveAsTemplateMutation();
  const [archiveBoard] = useArchiveBoardMutation();

  const [showArchivedPanel, setShowArchivedPanel] = useState(false);
  const [showActivitySidebar, setShowActivitySidebar] = useState(false);

  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: {
        distance: 5,
      },
    })
  );

  const board = boardData?.data;
  const lists = board?.lists ?? [];

  const { data: workspaceData } = useGetWorkspaceQuery(board?.workspaceId ?? 0, {
    skip: !board?.workspaceId,
  });
  const canManageBoard =
    workspaceData?.data.currentUserRole === 'OWNER' ||
    workspaceData?.data.currentUserRole === 'ADMIN' ||
    workspaceData?.data.currentUserRole === 'MEMBER';

  const { data: membersData } = useGetWorkspaceMembersQuery(board?.workspaceId ?? 0, {
    skip: !board?.workspaceId,
  });

  const [newListName, setNewListName] = useState('');
  const [showAddList, setShowAddList] = useState(false);
  const [boardName, setBoardName] = useState('');
  const [boardDescription, setBoardDescription] = useState('');
  const [isEditBoardModalOpen, setIsEditBoardModalOpen] = useState(false);
  const [isSaveAsTemplateModalOpen, setIsSaveAsTemplateModalOpen] = useState(false);
  const [templateName, setTemplateName] = useState('');
  const [templateDescription, setTemplateDescription] = useState('');
  const [editingList, setEditingList] = useState<TaskList | null>(null);
  const [listName, setListName] = useState('');
  
  // Phase 2: Card Detail Modal State
  const [selectedCard, setSelectedCard] = useState<CardItem | null>(null);
  const [activeCard, setActiveCard] = useState<CardItem | null>(null);

  // Effect to handle cardId from URL (for notifications)
  useEffect(() => {
    if (cardIdFromUrl && lists.length > 0) {
      const card = lists.flatMap(l => l.cards).find(c => c.id === Number(cardIdFromUrl));
      if (card) {
        setSelectedCard(card);
      }
    }
  }, [cardIdFromUrl, lists]);

  const handleCloseCardModal = () => {
    setSelectedCard(null);
    if (searchParams.has('cardId')) {
      const newParams = new URLSearchParams(searchParams);
      newParams.delete('cardId');
      setSearchParams(newParams);
    }
  };

  const handleActivityCardClick = (cardId: number) => {
    const card = lists.flatMap(l => l.cards).find(c => c.id === cardId);
    if (card) {
      setSelectedCard(card);
      setShowActivitySidebar(false);
    }
  };

  // Phase 2: Background Picker State
  const [showBackgroundPicker, setShowBackgroundPicker] = useState(false);

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

  const handleSaveAsTemplate = async () => {
    if (!templateName.trim()) return;
    try {
      await saveAsTemplate({
        boardId: id,
        name: templateName.trim(),
        description: templateDescription.trim() || undefined,
      }).unwrap();
      setIsSaveAsTemplateModalOpen(false);
      alert('Đã lưu bảng thành bản mẫu thành công!');
    } catch (err: any) {
      alert(err.data?.message || 'Không thể lưu bản mẫu');
    }
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
    if (confirm('Xóa danh sách này và tất cả thẻ bên trong?')) {
      await deleteList({ id: listId, boardId: id }).unwrap();
    }
  };

  const handleArchiveBoard = async () => {
    if (!board) return;
    if (confirm('Lưu trữ bảng này? Bảng sẽ được chuyển vào mục lưu trữ.')) {
      await archiveBoard(id).unwrap();
      navigate(`/workspace/${board.workspaceId}`);
    }
  };

  const handleDragStart = (event: any) => {
    const { active } = event;
    const cardId = Number(active.id);
    const card = lists.flatMap(l => l.cards).find(c => c.id === cardId);
    if (card) {
      setActiveCard(card);
    }
  };

  const handleDragEnd = async (event: DragEndEvent) => {
    const { active, over } = event;
    setActiveCard(null);
    if (!over) return;

    const cardId = Number(active.id);
    const overId = over.id.toString();

    // Check if dropped on archive zone
    if (overId === 'archive-zone') {
      try {
        await archiveCard({ id: cardId, boardId: id }).unwrap();
      } catch (err) {
        console.error('Failed to archive card:', err);
      }
      return;
    }

    // Find target list and position
    let targetListId: number | null = null;
    let newPosition = 0;

    if (overId.startsWith('list-')) {
      targetListId = Number(overId.replace('list-', ''));
      // Find the list and put at the end
      const targetList = lists.find(l => l.id === targetListId);
      newPosition = targetList?.cards.length ?? 0;
    } else {
      // It's a card id or similar
      const targetCardId = Number(overId);
      const targetList = lists.find(l => l.cards.some(c => c.id === targetCardId));
      if (targetList) {
        targetListId = targetList.id;
        const index = targetList.cards.findIndex(c => c.id === targetCardId);
        newPosition = index !== -1 ? index : targetList.cards.length;
      }
    }

    if (targetListId && cardId) {
      const card = lists.flatMap(l => l.cards).find(c => c.id === cardId);
      // Only call API if list or position changed
      if (card && (card.listId !== targetListId || card.position !== newPosition)) {
        try {
          await moveCard({ 
            id: cardId, 
            boardId: id, 
            targetListId, 
            position: newPosition 
          }).unwrap();
        } catch (err) {
          console.error('Failed to move card:', err);
        }
      }
    }
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

  // Close background picker when clicking outside
  useEffect(() => {
    const handleClickOutside = () => setShowBackgroundPicker(false);
    if (showBackgroundPicker) {
      window.addEventListener('click', handleClickOutside);
    }
    return () => window.removeEventListener('click', handleClickOutside);
  }, [showBackgroundPicker]);

  if (isLoading) return <div className={styles.loading}>Đang tải bảng...</div>;
  if (!board) return <div className={styles.loading}>Không tìm thấy bảng</div>;

  // Update selectedCard if board data changes (to keep modal in sync)
  const currentCard = selectedCard 
    ? lists.flatMap(l => l.cards).find(c => c.id === selectedCard.id) 
    : null;

  const isImageUrl = board.background?.startsWith('http') || board.background?.startsWith('/api/v1/files/');

  const containerStyle: React.CSSProperties = {
    backgroundImage: isImageUrl 
      ? `linear-gradient(rgba(0, 0, 0, 0.3), rgba(0, 0, 0, 0.3)), url(${board.background})` 
      : 'none',
    backgroundColor: board.background?.startsWith('#') ? board.background : undefined,
    backgroundSize: 'cover',
    backgroundPosition: 'center',
    backgroundRepeat: 'no-repeat',
    backgroundAttachment: 'fixed',
  };

  return (
    <div className={`${styles.container} ${isFiltering ? 'is-filtering' : ''}`} style={containerStyle}>
      <header className={styles.header}>
        <button className={styles.backBtn} onClick={() => navigate(-1)}>
          <FiChevronLeft /> <span>Quay lại</span>
        </button>
        <div className={styles.boardMeta}>
          {board.description ? <span className={styles.boardDesc}>{board.description}</span> : null}
        </div>
        <div className={styles.boardActions}>
          {canManageBoard ? (
            <div className={styles.toolbarGroup}>
              <button className="btn btn-outline" style={{ color: 'white', borderColor: 'rgba(255,255,255,0.2)' }} onClick={() => navigate(`/board/${id}/calendar`)}>
                <FiCalendar /> <span>Lịch</span>
              </button>
              <button className="btn btn-outline" style={{ color: 'white', borderColor: 'rgba(255,255,255,0.2)' }} onClick={() => navigate(`/board/${id}/timeline`)}>
                <FiClock /> <span>Dòng thời gian</span>
              </button>
              <button 
                className="btn btn-outline" 
                style={{ color: 'white', borderColor: 'rgba(255,255,255,0.2)' }} 
                onClick={() => setShowActivitySidebar(true)}
              >
                <FiActivity /> <span>Hoạt động</span>
              </button>
              <button className="btn btn-outline" style={{ color: 'white', borderColor: 'rgba(255,255,255,0.2)' }} onClick={() => navigate(`/board/${id}/analytics`)}>
                <FiPieChart /> <span>Thống kê</span>
              </button>
              <div className={styles.pickerContainer}>
                <button 
                  className={styles.settingsBtn} 
                  onClick={(e) => {
                    e.stopPropagation();
                    setShowBackgroundPicker(!showBackgroundPicker);
                  }}
                >
                  <FiImage /> <span>Hình nền</span>
                </button>
                {showBackgroundPicker && (
                  <div className={styles.backgroundPickerWrapper} onClick={(e) => e.stopPropagation()}>
                    <BackgroundPicker 
                      boardId={id} 
                      currentBackground={board.background} 
                    />
                  </div>
                )}
              </div>
              <button className="btn btn-outline" style={{ color: 'white', borderColor: 'rgba(255,255,255,0.2)' }} onClick={openBoardEditModal}>
                <FiSettings /> <span>Sửa bảng</span>
              </button>
              <button 
                className="btn btn-outline" 
                style={{ color: 'white', borderColor: 'rgba(255,255,255,0.2)' }} 
                onClick={() => {
                  setTemplateName(`${board.name} Bản mẫu`);
                  setTemplateDescription(board.description ?? '');
                  setIsSaveAsTemplateModalOpen(true);
                }}
              >
                <FiCopy /> <span>Lưu bản mẫu</span>
              </button>
              <button 
                className="btn btn-outline" 
                style={{ color: 'white', borderColor: 'rgba(255,255,255,0.2)' }} 
                onClick={() => setShowArchivedPanel(true)}
              >
                <FiArchive /> <span>Đã lưu trữ</span>
              </button>
              <button 
                className="btn btn-outline" 
                style={{ color: '#ff4d4f', borderColor: 'rgba(255,77,79,0.2)' }} 
                onClick={handleArchiveBoard}
              >
                <FiArchive /> <span>Ẩn bảng</span>
              </button>
            </div>
          ) : null}
        </div>
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

      <DndContext 
        sensors={sensors} 
        collisionDetection={closestCorners}
        onDragStart={handleDragStart}
        onDragEnd={handleDragEnd}
      >
        <div className={styles.kanban}>
          {lists.map((list) => (
            <BoardListColumn
              key={list.id}
              list={list}
              onEditList={handleOpenEditList}
              onDeleteList={(listId) => void handleDeleteList(listId)}
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
                  placeholder="Nhập tên danh sách..."
                  className={styles.addListInput}
                  autoFocus
                  onKeyDown={(e) => e.key === 'Enter' && void handleAddList()}
                />
                <div className={styles.addListActions}>
                  <button className="btn btn-primary btn-sm" onClick={() => void handleAddList()}>Thêm</button>
                  <button className="btn btn-outline btn-sm" onClick={() => setShowAddList(false)}>Hủy</button>
                </div>
              </div>
            ) : (
              <button className={styles.addListBtn} onClick={() => setShowAddList(true)}>+ Thêm danh sách</button>
            )}
          </div>
        </div>
        <DragOverlay dropAnimation={{
          sideEffects: defaultDropAnimationSideEffects({
            styles: {
              active: {
                opacity: '0.5',
              },
            },
          }),
        }}>
          {activeCard ? (
            <div className={styles.dragOverlayWrapper}>
              <SortableCard 
                card={activeCard} 
                onCardClick={() => {}} 
                priorityColor={priorityColor} 
                matchedCardIds={null} 
              />
            </div>
          ) : null}
        </DragOverlay>
        <ArchiveDropZone isDragging={activeCard !== null} />
      </DndContext>

      {isEditBoardModalOpen ? (
        <EntityModal
          title="Chỉnh sửa bảng"
          nameLabel="Tên bảng"
          nameValue={boardName}
          namePlaceholder="Nhập tên bảng"
          descriptionValue={boardDescription}
          onNameChange={setBoardName}
          onDescriptionChange={setBoardDescription}
          onClose={() => setIsEditBoardModalOpen(false)}
          onSubmit={() => void handleSaveBoard()}
          submitLabel="Lưu thay đổi"
          isSubmitting={isUpdatingBoard}
        />
      ) : null}

      {editingList ? (
        <EntityModal
          title="Chỉnh sửa danh sách"
          nameLabel="Tên danh sách"
          nameValue={listName}
          namePlaceholder="Nhập tên danh sách"
          onNameChange={setListName}
          onClose={() => setEditingList(null)}
          onSubmit={() => void handleSaveList()}
          submitLabel="Lưu thay đổi"
          isSubmitting={isUpdatingList}
          showDescription={false}
        />
      ) : null}

      {selectedCard && currentCard ? (
        <CardDetailModal
          card={currentCard}
          boardId={id}
          workspaceId={board.workspaceId}
          onClose={handleCloseCardModal}
          priorityColor={priorityColor}
        />
      ) : null}

      {isSaveAsTemplateModalOpen ? (
        <EntityModal
          title="Lưu thành bản mẫu"
          nameLabel="Tên bản mẫu"
          nameValue={templateName}
          namePlaceholder="vd: Bản mẫu phát triển phần mềm"
          descriptionValue={templateDescription}
          onNameChange={setTemplateName}
          onDescriptionChange={setTemplateDescription}
          onClose={() => setIsSaveAsTemplateModalOpen(false)}
          onSubmit={() => void handleSaveAsTemplate()}
          submitLabel="Lưu bản mẫu"
          isSubmitting={isSavingAsTemplate}
        />
      ) : null}

      {showArchivedPanel && (
        <ArchivedItemsPanel 
          boardId={id} 
          onClose={() => setShowArchivedPanel(false)} 
        />
      )}

      <BoardActivitySidebar 
        boardId={id}
        isOpen={showActivitySidebar}
        onClose={() => setShowActivitySidebar(false)}
        onCardClick={handleActivityCardClick}
      />
    </div>
  );
}

export default BoardPage;
