import { useNavigate } from 'react-router-dom';
import { logout } from '../features/auth/authSlice';
import { useAppDispatch, useAppSelector } from '../hooks/useRedux';
import styles from './DashboardPage.module.css';

function DashboardPage() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const user = useAppSelector((state) => state.auth.user);

  const handleLogout = () => {
    dispatch(logout());
    navigate('/login');
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
          <p>Your workspaces and boards will appear here.</p>
        </div>

        <div className={styles.emptyState}>
          <div className={styles.emptyIcon}>📋</div>
          <h2>No workspaces yet</h2>
          <p>Create your first workspace to start organizing tasks.</p>
          <button className="btn btn-primary">+ Create Workspace</button>
        </div>
      </main>
    </div>
  );
}

export default DashboardPage;
