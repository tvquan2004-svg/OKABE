import { useEffect, useState, type MouseEvent } from 'react';
import {
  DndContext,
  MouseSensor,
  TouchSensor,
  closestCenter,
  useSensor,
  useSensors,
  type DragEndEvent,
} from '@dnd-kit/core';
import {
  SortableContext,
  arrayMove,
  rectSortingStrategy,
} from '@dnd-kit/sortable';
import { useNavigate, useParams } from 'react-router-dom';
import EntityModal from '../components/common/EntityModal';
import MemberModal from '../components/workspace/MemberModal';
import SortableBoardCard from '../components/workspace/SortableBoardCard';
import {
  type Board,
  useCreateBoardMutation,
  useDeleteBoardMutation,
  useGetBoardsQuery,
  useReorderBoardsMutation,
  useUpdateBoardMutation,
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
  const [createBoard, { isLoading: isCreatingBoard }] = useCreateBoardMutation();
  const [updateBoard, { isLoading: isUpdatingBoard }] = useUpdateBoardMutation();
  const [reorderBoards] = useReorderBoardsMutation();
  const [deleteBoard] = useDeleteBoardMutation();
  const [updateWorkspace, { isLoading: isUpdatingWorkspace }] = useUpdateWorkspaceMutation();

  const [orderedBoards, setOrderedBoards] = useState<Board[]>([]);
  const [isCreateBoardModalOpen, setIsCreateBoardModalOpen] = useState(false);
  const [isEditWorkspaceModalOpen, setIsEditWorkspaceModalOpen] = useState(false);
  const [isMemberModalOpen, setIsMemberModalOpen] = useState(false);
  const [editingBoard, setEditingBoard] = useState<Board | null>(null);
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

  const handleSaveBoard = async () => {
    if (!boardName.trim()) {
      return;
    }

    if (editingBoard) {
      await updateBoard({
        id: editingBoard.id,
        body: {
          name: boardName.trim(),
          description: boardDescription.trim() || null,
        },
      }).unwrap();
      setEditingBoard(null);
      return;
    }

    const background = bgColors[Math.floor(Math.random() * bgColors.length)];
    await createBoard({
      workspaceId: id,
      name: boardName.trim(),
      description: boardDescription.trim() || undefined,
      background,
    }).unwrap();
    setIsCreateBoardModalOpen(false);
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
      alert(err.data?.message || 'Failed to update workspace');
    }
  };

  const handleDeleteBoard = async (boardId: number, event: MouseEvent<HTMLButtonElement>) => {
    event.stopPropagation();
    if (confirm('Delete this board?')) {
      await deleteBoard(boardId).unwrap();
    }
  };

  const handleEditBoard = (board: Board, event: MouseEvent<HTMLButtonElement>) => {
    event.stopPropagation();
    openEditBoardModal(board);
  };

  const handleDragEnd = async (event: DragEndEvent) => {
    if (!canEditBoards || !event.over || event.active.id === event.over.id) {
      return;
    }

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
            Back to dashboard
          </button>
          <div className={styles.wsInfo}>
            <h1>{workspace?.name ?? 'Workspace'}</h1>
            {workspace?.description ? <p>{workspace.description}</p> : null}
          </div>
        </div>
        <div className={styles.headerActions}>
          <button className="btn btn-outline" onClick={() => setIsMemberModalOpen(true)}>
            Members ({workspace?.memberCount ?? 0})
          </button>
          {canManageWorkspace ? (
            <button className="btn btn-outline" onClick={() => setIsEditWorkspaceModalOpen(true)}>
              Edit workspace
            </button>
          ) : null}
        </div>
      </header>

      <main className={styles.main}>
        <div className={styles.sectionHeader}>
          <div>
            <h2>Boards</h2>
            {canEditBoards ? (
              <p className={styles.muted}>Drag boards to reorder them.</p>
            ) : null}
          </div>
          <button className="btn btn-primary" onClick={openCreateBoardModal}>
            + New Board
          </button>
        </div>

        {isLoading ? (
          <p className={styles.muted}>Loading boards...</p>
        ) : orderedBoards.length === 0 ? (
          <div className={styles.emptyState}>
            <div className={styles.emptyIcon}>Boards</div>
            <h3>No boards yet</h3>
            <p>Create your first board to start organizing tasks.</p>
            <button className="btn btn-primary" onClick={openCreateBoardModal}>
              + Create Board
            </button>
          </div>
        ) : (
          <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
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
          </DndContext>
        )}
      </main>

      {isCreateBoardModalOpen ? (
        <EntityModal
          title="Create board"
          nameLabel="Board name"
          nameValue={boardName}
          namePlaceholder="My board"
          descriptionValue={boardDescription}
          onNameChange={setBoardName}
          onDescriptionChange={setBoardDescription}
          onClose={() => setIsCreateBoardModalOpen(false)}
          onSubmit={() => void handleSaveBoard()}
          submitLabel="Create"
          isSubmitting={isCreatingBoard}
        />
      ) : null}

      {editingBoard ? (
        <EntityModal
          title="Edit board"
          nameLabel="Board name"
          nameValue={boardName}
          namePlaceholder="My board"
          descriptionValue={boardDescription}
          onNameChange={setBoardName}
          onDescriptionChange={setBoardDescription}
          onClose={() => setEditingBoard(null)}
          onSubmit={() => void handleSaveBoard()}
          submitLabel="Save changes"
          isSubmitting={isUpdatingBoard}
        />
      ) : null}

      {isEditWorkspaceModalOpen ? (
        <EntityModal
          title="Edit workspace"
          nameLabel="Workspace name"
          nameValue={workspaceName}
          namePlaceholder="Workspace name"
          descriptionValue={workspaceDescription}
          onNameChange={setWorkspaceName}
          onDescriptionChange={setWorkspaceDescription}
          onClose={() => setIsEditWorkspaceModalOpen(false)}
          onSubmit={() => void handleSaveWorkspace()}
          submitLabel="Save changes"
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
