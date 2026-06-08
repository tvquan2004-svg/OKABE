import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAppSelector } from '../hooks/useRedux';
import {
  useGetWorkspacesQuery,
  useCreateWorkspaceMutation,
  useDeleteWorkspaceMutation,
} from '../services/workspaceApi';
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
    if (confirm(`Xóa không gian làm việc "${ws.name}"? Hành động này không thể hoàn tác.`)) {
      await deleteWorkspace(ws.id).unwrap();
    }
  };

  return (
    <div className={styles.pageContent}>
      <header className={styles.hero}>
        <div className={styles.welcome}>
          <p className={styles.greeting}>Chào mừng trở lại</p>
          <h1 className={styles.userName}>{user?.username}</h1>
          <p className={styles.subtitle}>Tổng quan về dự án của bạn hôm nay.</p>
        </div>
        
        <div className={styles.statsBar}>
          <div className={styles.statCard}>
            <div className={styles.statIcon}>
              <FiBriefcase />
            </div>
            <div className={styles.statInfo}>
              <span className={styles.statValue}>{workspaces.length}</span>
              <span className={styles.statLabel}>Không gian làm việc</span>
            </div>
          </div>
          <div className={styles.statCard}>
            <div className={styles.statIcon}>
              <FiUsers />
            </div>
            <div className={styles.statInfo}>
              <span className={styles.statValue}>
                {workspaces.reduce((acc, ws) => acc + ws.memberCount, 0)}
              </span>
              <span className={styles.statLabel}>Tổng thành viên</span>
            </div>
          </div>
          <div className={styles.statCard}>
            <div className={styles.statIcon}>
              <FiPieChart />
            </div>
            <div className={styles.statInfo}>
              <span className={styles.statValue}>
                {workspaces.reduce((acc, ws) => acc + (ws.boardCount || 0), 0)}
              </span>
              <span className={styles.statLabel}>Bảng đang hoạt động</span>
            </div>
          </div>
        </div>
      </header>

      <div className={styles.searchSection}>
        <div className={styles.searchBar}>
          <FiSearch className={styles.searchIcon} />
          <input 
            type="text" 
            placeholder="Tìm kiếm..." 
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>
        <button className={styles.createBtn} onClick={() => setShowModal(true)}>
          <FiPlus />
          <span>Không gian mới</span>
        </button>
      </div>

      <section className={styles.workspaceSection}>
        <div className={styles.sectionHeader}>
          <h2>Không gian làm việc của bạn</h2>
        </div>

        {isLoading ? (
          <div className={styles.loadingSkeleton}>
            {[1, 2, 3].map(i => <div key={i} className={styles.skeletonCard} />)}
          </div>
        ) : filteredWorkspaces.length === 0 ? (
          <div className={styles.emptyState}>
            {searchQuery ? (
              <>
                <p className={styles.emptyDesc}>
                  Không có kết quả cho "<strong>{searchQuery}</strong>"
                </p>
              </>
            ) : (
              <>
                <h3 className={styles.emptyTitle}>Chưa có không gian làm việc</h3>
                <p className={styles.emptyDesc}>
                  Tạo không gian làm việc đầu tiên để bắt đầu sắp xếp công việc.
                </p>
                <button className={styles.createBtn} onClick={() => setShowModal(true)}>
                  <FiPlus />
                  <span>Tạo không gian làm việc</span>
                </button>
              </>
            )}
          </div>
        ) : (
          <div className={styles.workspaceGrid}>
            {filteredWorkspaces.map((ws) => (
              <div key={ws.id} className={styles.workspaceCard} onClick={() => navigate(`/workspace/${ws.id}`)}>
                <div className={styles.wsBody}>
                  <div className={styles.wsIcon}>
                    {ws.name.charAt(0).toUpperCase()}
                  </div>
                  <div className={styles.wsInfo}>
                    <div className={styles.wsTop}>
                      <span className={styles.wsName}>{ws.name}</span>
                      <span className={styles.wsRole}>
                        {ws.currentUserRole === 'OWNER' ? 'Chủ sở hữu' : 
                         ws.currentUserRole === 'ADMIN' ? 'Quản trị viên' : 'Thành viên'}
                      </span>
                    </div>
                    <span className={styles.wsSlug}>okabe.io/{ws.slug}</span>
                    {ws.description && (
                      <p className={styles.wsDesc}>{ws.description}</p>
                    )}
                  </div>
                </div>
                <div className={styles.wsBottom}>
                  <div className={styles.wsMeta}>
                    <FiUsers size={13} />
                    <span>{ws.memberCount}</span>
                    <FiArchive size={13} />
                    <span>{ws.boardCount || 0}</span>
                  </div>
                  {ws.currentUserRole === 'OWNER' && (
                    <button
                      className={styles.deleteBtn}
                      onClick={(e) => { e.stopPropagation(); handleDelete(ws); }}
                      title="Xóa không gian làm việc"
                    >
                      <FiTrash2 size={14} />
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
              <h2>Tạo không gian làm việc</h2>
              <p>Không gian làm việc là nơi bạn cộng tác với nhóm và quản lý dự án.</p>
            </div>
            <div className={styles.modalField}>
              <label>Tên không gian làm việc *</label>
              <input
                type="text"
                value={newName}
                onChange={(e) => setNewName(e.target.value)}
                placeholder="vd: Đội ngũ Kỹ thuật"
                className={styles.modalInput}
                autoFocus
              />
            </div>
            <div className={styles.modalField}>
              <label>Mô tả (Tùy chọn)</label>
              <textarea
                value={newDesc}
                onChange={(e) => setNewDesc(e.target.value)}
                placeholder="Không gian này dùng để làm gì?"
                className={styles.modalTextarea}
                rows={3}
              />
            </div>
            <div className={styles.modalActions}>
              <button className={styles.modalBtn} onClick={() => setShowModal(false)}>
                Hủy
              </button>
              <button
                className={styles.modalBtnPrimary}
                onClick={handleCreate}
                disabled={isCreating || !newName.trim()}
              >
                {isCreating ? 'Đang tạo...' : 'Tạo không gian'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default DashboardPage;
