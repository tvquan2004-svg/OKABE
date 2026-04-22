import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAppSelector } from '../hooks/useRedux';
import {
  useGetWorkspacesQuery,
  useCreateWorkspaceMutation,
  useDeleteWorkspaceMutation,
} from '../services/workspaceApi';
import { useGetArchivedBoardsQuery } from '../services/boardApi';
import type { Workspace } from '../services/workspaceApi';
import { FiPlus, FiBriefcase, FiUsers, FiTrash2, FiSearch, FiArchive, FiPieChart } from 'react-icons/fi';
import styles from './DashboardPage.module.css';

function DashboardPage() {
  const navigate = useNavigate();
  const user = useAppSelector((state) => state.auth.user);

  const { data: workspacesData, isLoading } = useGetWorkspacesQuery();
  const [createWorkspace, { isLoading: isCreating }] = useCreateWorkspaceMutation();
  const [deleteWorkspace] = useDeleteWorkspaceMutation();

  const [showModal, setShowModal] = useState(false);
  const [newName, setNewName] = useState('');
  const [newDesc, setNewDesc] = useState('');
  const [searchQuery, setSearchQuery] = useState('');

  const workspaces = workspacesData?.data ?? [];
  
  const filteredWorkspaces = workspaces.filter(ws => 
    ws.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    ws.slug.toLowerCase().includes(searchQuery.toLowerCase())
  );

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
    <div className={styles.pageContent}>
      <header className={styles.hero}>
        <div className={styles.welcome}>
          <h1>Welcome back, <span className={styles.gradient}>{user?.username}</span> 👋</h1>
          <p>Here's what's happening with your projects today.</p>
        </div>
        
        <div className={styles.statsBar}>
          <div className={styles.statCard}>
            <div className={`${styles.statIcon} ${styles.blue}`}>
              <FiBriefcase />
            </div>
            <div className={styles.statInfo}>
              <span className={styles.statLabel}>Workspaces</span>
              <span className={styles.statValue}>{workspaces.length}</span>
            </div>
          </div>
          <div className={styles.statCard}>
            <div className={`${styles.statIcon} ${styles.green}`}>
              <FiUsers />
            </div>
            <div className={styles.statInfo}>
              <span className={styles.statLabel}>Total Members</span>
              <span className={styles.statValue}>
                {workspaces.reduce((acc, ws) => acc + ws.memberCount, 0)}
              </span>
            </div>
          </div>
          <div className={styles.statCard}>
            <div className={`${styles.statIcon} ${styles.purple}`}>
              <FiPieChart />
            </div>
            <div className={styles.statInfo}>
              <span className={styles.statLabel}>Active Boards</span>
              <span className={styles.statValue}>
                {workspaces.reduce((acc, ws) => acc + (ws.boardCount || 0), 0)}
              </span>
            </div>
          </div>
        </div>
      </header>

      <div className={styles.searchSection}>
        <div className={styles.searchBar}>
          <FiSearch className={styles.searchIcon} />
          <input 
            type="text" 
            placeholder="Search workspaces..." 
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>
        <button className="btn btn-primary" onClick={() => setShowModal(true)}>
          <FiPlus /> New Workspace
        </button>
      </div>

      <section className={styles.workspaceSection}>
        <div className={styles.sectionHeader}>
          <h2>Your Workspaces</h2>
        </div>

        {isLoading ? (
          <div className={styles.loadingSkeleton}>
            {[1, 2, 3].map(i => <div key={i} className={styles.skeletonCard} />)}
          </div>
        ) : filteredWorkspaces.length === 0 ? (
          <div className={styles.emptyState}>
            <div className={styles.emptyIcon}>📂</div>
            <h2>{searchQuery ? 'No results found' : 'No workspaces yet'}</h2>
            <p>
              {searchQuery 
                ? `We couldn't find any workspace matching "${searchQuery}"` 
                : 'Create your first workspace to start organizing tasks.'}
            </p>
            {!searchQuery && (
              <button className="btn btn-primary" onClick={() => setShowModal(true)}>
                + Create Workspace
              </button>
            )}
          </div>
        ) : (
          <div className={styles.workspaceGrid}>
            {filteredWorkspaces.map((ws) => (
              <div key={ws.id} className={styles.workspaceCard} onClick={() => navigate(`/workspace/${ws.id}`)}>
                <div className={styles.wsCardBody}>
                  <div className={styles.wsIcon}>
                    {ws.name.charAt(0).toUpperCase()}
                  </div>
                  <div className={styles.wsContent}>
                    <div className={styles.wsHeader}>
                      <h3>{ws.name}</h3>
                      <span className={styles.wsRole}>{ws.currentUserRole}</span>
                    </div>
                    <span className={styles.wsSlug}>okabe.io/{ws.slug}</span>
                    {ws.description && (
                      <p className={styles.wsDesc}>{ws.description}</p>
                    )}
                  </div>
                </div>
                <div className={styles.wsFooter}>
                  <div className={styles.wsMeta}>
                    <FiUsers /> <span>{ws.memberCount}</span>
                    <span className={styles.separator}>•</span>
                    <FiArchive /> <span>{ws.boardCount || 0} boards</span>
                  </div>
                  {ws.currentUserRole === 'OWNER' && (
                    <button
                      className={styles.deleteBtn}
                      onClick={(e) => { e.stopPropagation(); handleDelete(ws); }}
                      title="Delete workspace"
                    >
                      <FiTrash2 />
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      {showModal && (
        <div className={styles.modalOverlay} onClick={() => setShowModal(false)}>
          <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
            <div className={styles.modalHeader}>
              <h2>Create Workspace</h2>
              <p>Workspaces are where your team collaborators and projects live.</p>
            </div>
            <div className={styles.modalField}>
              <label>Workspace Name *</label>
              <input
                type="text"
                value={newName}
                onChange={(e) => setNewName(e.target.value)}
                placeholder="e.g. Engineering Team"
                className={styles.modalInput}
                autoFocus
              />
            </div>
            <div className={styles.modalField}>
              <label>Description (Optional)</label>
              <textarea
                value={newDesc}
                onChange={(e) => setNewDesc(e.target.value)}
                placeholder="What is this workspace about?"
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
                {isCreating ? 'Creating...' : 'Create Workspace'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default DashboardPage;
