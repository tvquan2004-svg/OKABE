import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useGetWorkspaceQuery } from '../services/workspaceApi';
import {
  useGetBoardsQuery,
  useCreateBoardMutation,
  useDeleteBoardMutation,
} from '../services/boardApi';
import styles from './WorkspacePage.module.css';

function WorkspacePage() {
  const { workspaceId } = useParams<{ workspaceId: string }>();
  const navigate = useNavigate();
  const id = Number(workspaceId);

  const { data: wsData } = useGetWorkspaceQuery(id);
  const { data: boardsData, isLoading } = useGetBoardsQuery(id);
  const [createBoard, { isLoading: isCreating }] = useCreateBoardMutation();
  const [deleteBoard] = useDeleteBoardMutation();

  const [showModal, setShowModal] = useState(false);
  const [boardName, setBoardName] = useState('');
  const [boardDesc, setBoardDesc] = useState('');

  const workspace = wsData?.data;
  const boards = boardsData?.data ?? [];

  const bgColors = ['#6366f1', '#8b5cf6', '#06b6d4', '#22c55e', '#f59e0b', '#ef4444', '#ec4899'];

  const handleCreate = async () => {
    if (!boardName.trim()) return;
    const bg = bgColors[Math.floor(Math.random() * bgColors.length)];
    await createBoard({ workspaceId: id, name: boardName, description: boardDesc || undefined, background: bg }).unwrap();
    setBoardName('');
    setBoardDesc('');
    setShowModal(false);
  };

  const handleDelete = async (boardId: number, e: React.MouseEvent) => {
    e.stopPropagation();
    if (confirm('Delete this board?')) {
      await deleteBoard(boardId).unwrap();
    }
  };

  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <button className={styles.backBtn} onClick={() => navigate('/dashboard')}>← Dashboard</button>
        <div className={styles.wsInfo}>
          <h1>{workspace?.name ?? 'Workspace'}</h1>
          {workspace?.description && <p>{workspace.description}</p>}
        </div>
      </header>

      <main className={styles.main}>
        <div className={styles.sectionHeader}>
          <h2>Boards</h2>
          <button className="btn btn-primary" onClick={() => setShowModal(true)}>+ New Board</button>
        </div>

        {isLoading ? (
          <p className={styles.muted}>Loading boards...</p>
        ) : boards.length === 0 ? (
          <div className={styles.emptyState}>
            <div className={styles.emptyIcon}>📋</div>
            <h3>No boards yet</h3>
            <p>Create your first board to start organizing tasks.</p>
            <button className="btn btn-primary" onClick={() => setShowModal(true)}>+ Create Board</button>
          </div>
        ) : (
          <div className={styles.boardGrid}>
            {boards.map((b) => (
              <div
                key={b.id}
                className={styles.boardCard}
                style={{ borderTopColor: b.background ?? '#6366f1' }}
                onClick={() => navigate(`/board/${b.id}`)}
              >
                <div className={styles.boardColorBar} style={{ background: b.background ?? '#6366f1' }} />
                <div className={styles.boardContent}>
                  <h3>{b.name}</h3>
                  {b.description && <p>{b.description}</p>}
                  <div className={styles.boardStats}>
                    <span>📋 {b.listCount ?? 0} list{(b.listCount ?? 0) !== 1 ? 's' : ''}</span>
                    <span>🗂️ {b.totalCards ?? 0} card{(b.totalCards ?? 0) !== 1 ? 's' : ''}</span>
                  </div>
                </div>
                <button className={styles.deleteBoardBtn} onClick={(e) => handleDelete(b.id, e)}>🗑️</button>
              </div>
            ))}
          </div>
        )}

        {/* Create Board Modal */}
        {showModal && (
          <div className={styles.modalOverlay} onClick={() => setShowModal(false)}>
            <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
              <h2>Create Board</h2>
              <div className={styles.modalField}>
                <label>Board Name *</label>
                <input
                  value={boardName}
                  onChange={(e) => setBoardName(e.target.value)}
                  placeholder="My Board"
                  className={styles.modalInput}
                  autoFocus
                />
              </div>
              <div className={styles.modalField}>
                <label>Description</label>
                <textarea
                  value={boardDesc}
                  onChange={(e) => setBoardDesc(e.target.value)}
                  placeholder="Optional..."
                  className={styles.modalTextarea}
                  rows={3}
                />
              </div>
              <div className={styles.modalActions}>
                <button className="btn btn-outline" onClick={() => setShowModal(false)}>Cancel</button>
                <button className="btn btn-primary" onClick={handleCreate} disabled={isCreating || !boardName.trim()}>
                  {isCreating ? 'Creating...' : 'Create'}
                </button>
              </div>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}

export default WorkspacePage;
