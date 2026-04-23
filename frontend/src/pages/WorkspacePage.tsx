import { useEffect, useState, type MouseEvent } from 'react';
import {
  DndContext,
  MouseSensor,
  TouchSensor,
  closestCenter,
  useSensor,
  useSensors,
  type DragEndEvent,
  DragOverlay,
  defaultDropAnimationSideEffects,
} from '@dnd-kit/core';
import { FiRotateCcw, FiTrash2, FiArchive, FiEye, FiEyeOff } from 'react-icons/fi';
import BoardArchiveZone from '../components/workspace/BoardArchiveZone';
import {
  SortableContext,
  arrayMove,
  rectSortingStrategy,
} from '@dnd-kit/sortable';
import { useNavigate, useParams } from 'react-router-dom';
import EntityModal from '../components/common/EntityModal';
import MemberModal from '../components/workspace/MemberModal';
import SortableBoardCard from '../components/workspace/SortableBoardCard';
import CreateBoardModal from '../components/workspace/CreateBoardModal';
import {
  type Board,
  useCreateBoardMutation,
  useDeleteBoardMutation,
  useGetBoardsQuery,
  useGetArchivedBoardsQuery,
  useReorderBoardsMutation,
  useUpdateBoardMutation,
  useRestoreBoardMutation,
} from '../services/boardApi';
import {
  useGetWorkspaceQuery,
  useUpdateWorkspaceMutation,
} from '../services/workspaceApi';
import styles from './WorkspacePage.module.css';

function WorkspacePage() {
  const { workspaceId } = useParams<{ workspaceId: string }>();
  const navigate = useNavigate();
  const id = Number(workspaceId);

  const { data: workspaceData } = useGetWorkspaceQuery(id);
  const { data: boardsData, isLoading } = useGetBoardsQuery(id);
  const { data: archivedBoardsData } = useGetArchivedBoardsQuery(id);
  const [createBoard, { isLoading: isCreatingBoard }] = useCreateBoardMutation();
  const [updateBoard, { isLoading: isUpdatingBoard }] = useUpdateBoardMutation();
  const [reorderBoards] = useReorderBoardsMutation();
  const [deleteBoard] = useDeleteBoardMutation();
  const [restoreBoard] = useRestoreBoardMutation();
  const [updateWorkspace, { isLoading: isUpdatingWorkspace }] = useUpdateWorkspaceMutation();

  const [orderedBoards, setOrderedBoards] = useState<Board[]>([]);
  const [showArchivedBoards, setShowArchivedBoards] = useState(false);
  const [isCreateBoardModalOpen, setIsCreateBoardModalOpen] = useState(false);
  const [isEditWorkspaceModalOpen, setIsEditWorkspaceModalOpen] = useState(false);
  const [isMemberModalOpen, setIsMemberModalOpen] = useState(false);
  const [editingBoard, setEditingBoard] = useState<Board | null>(null);
  const [activeBoard, setActiveBoard] = useState<Board | null>(null);
  const [boardName, setBoardName] = useState('');
  const [boardDescription, setBoardDescription] = useState('');
  const [workspaceName, setWorkspaceName] = useState('');
  const [workspaceDescription, setWorkspaceDescription] = useState('');

  const workspace = workspaceData?.data;
  const boards = boardsData?.data;
  const canManageWorkspace =
    workspace?.currentUserRole === 'OWNER' || workspace?.currentUserRole === 'ADMIN';
  const canEditBoards =
    canManageWorkspace || workspace?.currentUserRole === 'MEMBER';

  const sensors = useSensors(
    useSensor(MouseSensor, {
      activationConstraint: {
        distance: 5,
      },
    }),
    useSensor(TouchSensor, {
      activationConstraint: {
        delay: 250,
        tolerance: 5,
      },
    })
  );

  useEffect(() => {
    if (boards) {
      setOrderedBoards(boards);
    }
  }, [boards]);

  useEffect(() => {
    if (!workspace) {
      return;
    }

    setWorkspaceName(workspace.name);
    setWorkspaceDescription(workspace.description ?? '');
  }, [workspace]);

  const bgColors = ['#6366f1', '#8b5cf6', '#06b6d4', '#22c55e', '#f59e0b', '#ef4444', '#ec4899'];

  const openCreateBoardModal = () => {
    setBoardName('');
    setBoardDescription('');
    setEditingBoard(null);
    setIsCreateBoardModalOpen(true);
  };

  const openEditBoardModal = (board: Board) => {
    setEditingBoard(board);
    setBoardName(board.name);
    setBoardDescription(board.description ?? '');
  };

  const handleSaveBoard = async (data: { name: string; description?: string; templateId?: number }) => {
    const background = bgColors[Math.floor(Math.random() * bgColors.length)];
    await createBoard({
      workspaceId: id,
      name: data.name,
      description: data.description,
      background,
      templateId: data.templateId,
    }).unwrap();
    setIsCreateBoardModalOpen(false);
  };

  const handleUpdateBoard = async () => {
    if (!boardName.trim() || !editingBoard) {
      return;
    }

    await updateBoard({
      id: editingBoard.id,
      body: {
        name: boardName.trim(),
        description: boardDescription.trim() || null,
      },
    }).unwrap();
    setEditingBoard(null);
  };

  const handleSaveWorkspace = async () => {
    if (!workspaceName.trim()) {
      return;
    }

    try {
      await updateWorkspace({
        id,
        body: {
          name: workspaceName.trim(),
          description: workspaceDescription.trim() || undefined,
        },
      }).unwrap();
      setIsEditWorkspaceModalOpen(false);
    } catch (err: any) {
      alert(err.data?.message || 'Không thể cập nhật không gian làm việc');
    }
  };

  const handleDeleteBoard = async (boardId: number, event: MouseEvent<HTMLButtonElement>) => {
    event.stopPropagation();
    if (confirm('Xóa bảng này vĩnh viễn? Hành động này không thể hoàn tác.')) {
      try {
        await deleteBoard(boardId).unwrap();
      } catch (err: any) {
        alert(err.data?.message || 'Không thể xóa bảng. Có thể bạn không có quyền.');
      }
    }
  };

  const handleRestoreBoard = async (boardId: number, event: MouseEvent<HTMLButtonElement>) => {
    event.stopPropagation();
    try {
      await restoreBoard(boardId).unwrap();
    } catch (err: any) {
      alert(err.data?.message || 'Không thể khôi phục bảng. Có thể bạn không có quyền.');
    }
  };

  const handleEditBoard = (board: Board, event: MouseEvent<HTMLButtonElement>) => {
    event.stopPropagation();
    openEditBoardModal(board);
  };

  const handleDragStart = (event: any) => {
    const { active } = event;
    const board = orderedBoards.find(b => b.id === active.id);
    if (board) setActiveBoard(board);
  };

  const handleDragEnd = async (event: DragEndEvent) => {
    setActiveBoard(null);
    const { active, over } = event;
    if (!canEditBoards || !over) {
      return;
    }

    // Handle Drop to Archive Zone
    if (over.id === 'archive-zone') {
      const boardId = Number(active.id);
      try {
        await updateBoard({
          id: boardId,
          body: { isArchived: true }
        }).unwrap();
        // Optional: Show a toast notification here
      } catch (err) {
        console.error('Failed to archive board:', err);
      }
      return;
    }

    if (active.id === over.id) return;

    const oldIndex = orderedBoards.findIndex((board) => board.id === event.active.id);
    const newIndex = orderedBoards.findIndex((board) => board.id === event.over?.id);

    if (oldIndex < 0 || newIndex < 0) {
      return;
    }

    const nextBoards = arrayMove(orderedBoards, oldIndex, newIndex);
    setOrderedBoards(nextBoards);

    try {
      await reorderBoards({
        workspaceId: id,
        orderedIds: nextBoards.map((board) => board.id),
      }).unwrap();
    } catch {
      setOrderedBoards(boards ?? []);
    }
  };

  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div className={styles.wsHeaderMain}>
          <button className={styles.backBtn} onClick={() => navigate('/dashboard')}>
            Quay lại bảng điều khiển
          </button>
          <div className={styles.headerFlex}>
            <div className={styles.wsInfo}>
              <h1>{workspace?.name ?? 'Không gian làm việc'}</h1>
              {workspace?.description ? <p>{workspace.description}</p> : <p>Không có mô tả cho không gian này</p>}
            </div>
            <div className={styles.headerActions}>
              <button className="btn btn-outline" onClick={() => setIsMemberModalOpen(true)}>
                Thành viên ({workspace?.memberCount ?? 0})
              </button>
              {canManageWorkspace ? (
                <button className="btn btn-outline" onClick={() => setIsEditWorkspaceModalOpen(true)}>
                  Thiết lập
                </button>
              ) : null}
            </div>
          </div>
        </div>
      </header>

      <main className={styles.main}>
        <div className={styles.sectionHeader}>
          <div>
            <h2>Các bảng công việc</h2>
            {canEditBoards ? (
              <p className={styles.muted}>Kéo thả các bảng để thay đổi thứ tự.</p>
            ) : null}
          </div>
          <button className="btn btn-primary" onClick={openCreateBoardModal}>
            + Bảng mới
          </button>
        </div>

        {isLoading ? (
          <p className={styles.muted}>Đang tải danh sách bảng...</p>
        ) : orderedBoards.length === 0 ? (
          <div className={styles.emptyState}>
            <div className={styles.emptyIcon}>Bảng</div>
            <h3>Chưa có bảng nào</h3>
            <p>Tạo bảng công việc đầu tiên của bạn để bắt đầu sắp xếp công việc.</p>
            <button className="btn btn-primary" onClick={openCreateBoardModal}>
              + Tạo bảng mới
            </button>
          </div>
        ) : (
          <DndContext 
            sensors={sensors} 
            collisionDetection={closestCenter} 
            onDragStart={handleDragStart}
            onDragEnd={handleDragEnd}
          >
            <SortableContext
              items={orderedBoards.map((board) => board.id)}
              strategy={rectSortingStrategy}
            >
              <div className={styles.boardGrid}>
                {orderedBoards.map((board) => (
                  <SortableBoardCard
                    key={board.id}
                    board={board}
                    canManage={canEditBoards}
                    canReorder={canEditBoards}
                    onOpen={(boardId) => navigate(`/board/${boardId}`)}
                    onEdit={handleEditBoard}
                    onDelete={handleDeleteBoard}
                  />
                ))}
              </div>
            </SortableContext>
            
            <BoardArchiveZone isDragging={!!activeBoard} />

            <DragOverlay dropAnimation={{
              sideEffects: defaultDropAnimationSideEffects({
                styles: {
                  active: {
                    opacity: '0.5',
                  },
                },
              }),
            }}>
              {activeBoard ? (
                <div style={{ width: '300px', cursor: 'grabbing' }}>
                  <SortableBoardCard
                    board={activeBoard}
                    canManage={false}
                    canReorder={false}
                    onOpen={() => {}}
                    onEdit={() => {}}
                    onDelete={() => {}}
                  />
                </div>
              ) : null}
            </DragOverlay>
          </DndContext>
        )}

        <div className={styles.archivedSection}>
          <button 
            className={styles.toggleArchivedBtn}
            onClick={() => setShowArchivedBoards(!showArchivedBoards)}
          >
            {showArchivedBoards ? <FiEyeOff /> : <FiEye />}
            {showArchivedBoards ? 'Ẩn' : 'Hiện'} bảng đã lưu trữ ({archivedBoardsData?.data.length ?? 0})
          </button>

          {showArchivedBoards && (
            <div className={styles.boardGrid} style={{ marginTop: '1rem', opacity: 0.7 }}>
              {archivedBoardsData?.data.length === 0 ? (
                <p className={styles.muted}>Không có bảng nào được lưu trữ.</p>
              ) : (
                archivedBoardsData?.data.map((board) => (
                  <div 
                    key={board.id} 
                    className={styles.archivedBoardCard}
                    style={{ 
                      '--board-accent': board.background?.startsWith('#') ? board.background : '#6366f1'
                    } as any}
                  >
                    <div className={styles.archivedContent}>
                      <div className={styles.archivedHeader}>
                        <FiArchive className={styles.archiveIcon} />
                        <h3>{board.name}</h3>
                      </div>
                      <div className={styles.archivedActions}>
                        <button 
                          className={styles.archiveActionBtn} 
                          onClick={(e) => handleRestoreBoard(board.id, e)}
                          title="Khôi phục bảng"
                        >
                          <FiRotateCcw size={16} />
                          <span>Khôi phục</span>
                        </button>
                        <button 
                          className={`${styles.archiveActionBtn} ${styles.archiveDeleteBtn}`}
                          onClick={(e) => handleDeleteBoard(board.id, e)}
                          title="Xóa vĩnh viễn"
                        >
                          <FiTrash2 size={16} />
                          <span>Xóa</span>
                        </button>
                      </div>
                    </div>
                  </div>
                ))
              )}
            </div>
          )}
        </div>
      </main>

      {isCreateBoardModalOpen ? (
        <CreateBoardModal
          workspaceId={id}
          isOpen={isCreateBoardModalOpen}
          isSubmitting={isCreatingBoard}
          onClose={() => setIsCreateBoardModalOpen(false)}
          onSubmit={handleSaveBoard}
        />
      ) : null}

      {editingBoard ? (
        <EntityModal
          title="Chỉnh sửa bảng"
          nameLabel="Tên bảng"
          nameValue={boardName}
          namePlaceholder="vd: Dự án của tôi"
          descriptionValue={boardDescription}
          onNameChange={setBoardName}
          onDescriptionChange={setBoardDescription}
          onClose={() => setEditingBoard(null)}
          onSubmit={() => void handleUpdateBoard()}
          submitLabel="Lưu thay đổi"
          isSubmitting={isUpdatingBoard}
        />
      ) : null}

      {isEditWorkspaceModalOpen ? (
        <EntityModal
          title="Chỉnh sửa không gian"
          nameLabel="Tên không gian làm việc"
          nameValue={workspaceName}
          namePlaceholder="Tên không gian"
          descriptionValue={workspaceDescription}
          onNameChange={setWorkspaceName}
          onDescriptionChange={setWorkspaceDescription}
          onClose={() => setIsEditWorkspaceModalOpen(false)}
          onSubmit={() => void handleSaveWorkspace()}
          submitLabel="Lưu thay đổi"
          isSubmitting={isUpdatingWorkspace}
        />
      ) : null}

      {isMemberModalOpen && workspace && (
        <MemberModal
          workspaceId={id}
          onClose={() => setIsMemberModalOpen(false)}
          currentUserRole={workspace.currentUserRole}
        />
      )}
    </div>
  );
}

export default WorkspacePage;
