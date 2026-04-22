import React from 'react';
import { NavLink, useParams } from 'react-router-dom';
import { 
  FiGrid, 
  FiLayers, 
  FiPlus, 
  FiArchive, 
  FiSettings, 
  FiChevronLeft, 
  FiChevronRight 
} from 'react-icons/fi';
import { useGetWorkspacesQuery } from '../../services/workspaceApi';
import styles from './Sidebar.module.css';

interface SidebarProps {
  isCollapsed: boolean;
  onToggle: () => void;
}

const Sidebar: React.FC<SidebarProps> = ({ isCollapsed, onToggle }) => {
  const { workspaceId } = useParams<{ workspaceId: string }>();
  const { data: workspacesRes } = useGetWorkspacesQuery();
  const workspaces = workspacesRes?.data ?? [];

  return (
    <aside className={`${styles.sidebar} ${isCollapsed ? styles.collapsed : ''}`}>
      <div className={styles.header}>
        {!isCollapsed && <span className={styles.logo}>OKABE</span>}
        <button className={styles.toggleBtn} onClick={onToggle}>
          {isCollapsed ? <FiChevronRight /> : <FiChevronLeft />}
        </button>
      </div>

      <nav className={styles.nav}>
        <div className={styles.section}>
          {!isCollapsed && <span className={styles.sectionTitle}>General</span>}
          <NavLink 
            to="/dashboard" 
            className={({ isActive }) => `${styles.navItem} ${isActive ? styles.active : ''}`}
            title="Dashboard"
          >
            <FiGrid />
            {!isCollapsed && <span>Dashboard</span>}
          </NavLink>
        </div>

        <div className={styles.section}>
          <div className={styles.sectionHeader}>
            {!isCollapsed && <span className={styles.sectionTitle}>Workspaces</span>}
          </div>
          {workspaces.map(ws => (
            <NavLink 
              key={ws.id}
              to={`/workspace/${ws.id}`}
              className={({ isActive }) => `${styles.navItem} ${isActive ? styles.active : ''}`}
              title={ws.name}
            >
              <div className={styles.wsIcon}>{ws.name.charAt(0).toUpperCase()}</div>
              {!isCollapsed && <span className={styles.wsName}>{ws.name}</span>}
            </NavLink>
          ))}
        </div>

        <div className={styles.section}>
          {!isCollapsed && <span className={styles.sectionTitle}>Other</span>}
          <NavLink 
            to="/settings" 
            className={({ isActive }) => `${styles.navItem} ${isActive ? styles.active : ''}`}
            title="Settings"
          >
            <FiSettings />
            {!isCollapsed && <span>Settings</span>}
          </NavLink>
        </div>
      </nav>

      <div className={styles.footer}>
        {!isCollapsed && (
          <div className={styles.footerInfo}>
            <p className={styles.version}>v1.0.0</p>
          </div>
        )}
      </div>
    </aside>
  );
};

export default Sidebar;
