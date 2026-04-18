import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { logout } from '../features/auth/authSlice';
import { useAppDispatch, useAppSelector } from '../hooks/useRedux';
import {
  useGetWorkspacesQuery,
  useCreateWorkspaceMutation,
  useDeleteWorkspaceMutation,
} from '../services/workspaceApi';
import type { Workspace } from '../services/workspaceApi';
import styles from './DashboardPage.module.css';

function DashboardPage() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const user = useAppSelector((state) => state.auth.user);

  const { data: workspacesData, isLoading } = useGetWorkspacesQuery();
  const [createWorkspace, { isLoading: isCreating }] = useCreateWorkspaceMutation();
  const [deleteWorkspace] = useDeleteWorkspaceMutation();

  const [showModal, setShowModal] = useState(false);
  const [newName, setNewName] = useState('');
  const [newDesc, setNewDesc] = useState('');

  const workspaces = workspacesData?.data ?? [];

  const handleLogout = () => {
    dispatch(logout());
    navigate('/login');
  };

  const handleCreate = async () => {
    if (!newName.trim()) return;
    try {
      await createWorkspace({ name: newName, description: newDesc || undefined }).unwrap();
      setShowModal(false);
      setNewName('');
      setNewDesc('');
    } catch {
      // Error handled by RTK Query
    }
  };

  const handleDelete = async (ws: Workspace) => {
    if (confirm(`Delete workspace "${ws.name}"? This cannot be undone.`)) {
      await deleteWorkspace(ws.id).unwrap();
    }
  };

  return (
    <div className={styles.container}>
      <header className={styles.topBar}>
        <div className={styles.logo}>
          <span>⚡</span>
          <span className={styles.logoText}>OKABE</span>
        </div>
        <div className={styles.userSection}>
          <div className={styles.avatar}>
            {user?.username?.charAt(0).toUpperCase()}
          </div>
          <span className={styles.username}>{user?.username}</span>
          <button onClick={handleLogout} className={styles.logoutBtn}>
            Logout
          </button>
        </div>
      </header>

      <main className={styles.main}>
        <div className={styles.welcome}>
          <h1>Welcome, <span className={styles.gradient}>{user?.username}</span> 👋</h1>
          <p>Manage your workspaces and boards.</p>
        </div>

        {/* Workspace List */}
        <div className={styles.sectionHeader}>
          <h2>Your Workspaces</h2>
          <button className="btn btn-primary" onClick={() => setShowModal(true)}>
            + New Workspace
          </button>
        </div>

        {isLoading ? (
          <div className={styles.loading}>Loading workspaces...</div>
        ) : workspaces.length === 0 ? (
          <div className={styles.emptyState}>
            <div className={styles.emptyIcon}>📋</div>
            <h2>No workspaces yet</h2>
            <p>Create your first workspace to start organizing tasks.</p>
            <button className="btn btn-primary" onClick={() => setShowModal(true)}>
              + Create Workspace
            </button>
          </div>
        ) : (
          <div className={styles.workspaceGrid}>
            {workspaces.map((ws) => (
              <div key={ws.id} className={styles.workspaceCard} onClick={() => navigate(`/workspace/${ws.id}`)}>
                <div className={styles.wsCardHeader}>
                  <div className={styles.wsIcon}>
                    {ws.name.charAt(0).toUpperCase()}
                  </div>
                  <div className={styles.wsInfo}>
                    <h3>{ws.name}</h3>
                    <span className={styles.wsSlug}>/{ws.slug}</span>
                  </div>
                </div>
                {ws.description && (
                  <p className={styles.wsDesc}>{ws.description}</p>
                )}
                <div className={styles.wsFooter}>
                  <span className={styles.wsMembers}>👥 {ws.memberCount} member{ws.memberCount > 1 ? 's' : ''}</span>
                  <span className={styles.wsRole}>{ws.currentUserRole}</span>
                  {ws.currentUserRole === 'OWNER' && (
                    <button
                      className={styles.deleteBtn}
                      onClick={(e) => { e.stopPropagation(); handleDelete(ws); }}
                      title="Delete workspace"
                    >
                      🗑️
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Create Modal */}
        {showModal && (
          <div className={styles.modalOverlay} onClick={() => setShowModal(false)}>
            <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
              <h2>Create Workspace</h2>
              <div className={styles.modalField}>
                <label>Name *</label>
                <input
                  type="text"
                  value={newName}
                  onChange={(e) => setNewName(e.target.value)}
                  placeholder="My Workspace"
                  className={styles.modalInput}
                  autoFocus
                />
              </div>
              <div className={styles.modalField}>
                <label>Description</label>
                <textarea
                  value={newDesc}
                  onChange={(e) => setNewDesc(e.target.value)}
                  placeholder="Optional description..."
                  className={styles.modalTextarea}
                  rows={3}
                />
              </div>
              <div className={styles.modalActions}>
                <button className="btn btn-outline" onClick={() => setShowModal(false)}>
                  Cancel
                </button>
                <button
                  className="btn btn-primary"
                  onClick={handleCreate}
                  disabled={isCreating || !newName.trim()}
                >
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

export default DashboardPage;
