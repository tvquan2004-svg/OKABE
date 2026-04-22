import React from 'react';
import { NavLink } from 'react-router-dom';
import { 
  FiGrid, 
  FiSettings, 
  FiChevronLeft, 
  FiChevronRight,
  FiX
} from 'react-icons/fi';
import { useGetWorkspacesQuery } from '../../services/workspaceApi';
import styles from './Sidebar.module.css';

interface SidebarProps {
  isCollapsed: boolean;
  onToggle: () => void;
  isMobileOpen?: boolean;
  onCloseMobile?: () => void;
}

const Sidebar: React.FC<SidebarProps> = ({ isCollapsed, onToggle, isMobileOpen, onCloseMobile }) => {
  const { data: workspacesRes } = useGetWorkspacesQuery();
  const workspaces = workspacesRes?.data ?? [];

  return (
    <aside className={`
      ${styles.sidebar} 
      ${isCollapsed ? styles.collapsed : ''} 
      ${isMobileOpen ? styles.mobileOpen : ''}
    `}>
      <div className={styles.header}>
        <span className={styles.logo}>OKABE</span>
        <div className={styles.headerActions}>
          <button className={styles.toggleBtn} onClick={onToggle}>
            {isCollapsed ? <FiChevronRight /> : <FiChevronLeft />}
          </button>
          <button className={styles.closeMobileBtn} onClick={onCloseMobile}>
            <FiX />
          </button>
        </div>
      </div>

      <nav className={styles.nav}>
        <div className={styles.section}>
          {!isCollapsed && <span className={styles.sectionTitle}>General</span>}
          <NavLink 
            to="/dashboard" 
            className={({ isActive }) => `${styles.navItem} ${isActive ? styles.active : ''}`}
            onClick={onCloseMobile}
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
              onClick={onCloseMobile}
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
            onClick={onCloseMobile}
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
