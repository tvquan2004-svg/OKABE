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
import logoImg from '../../../favicon.png';

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
        <div className={styles.logoContainer}>
          <img src={logoImg} className={styles.logoImg} alt="Logo" />
          {!isCollapsed && <span className={styles.logoText}>OKABE</span>}
        </div>
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
          {!isCollapsed && <span className={styles.sectionTitle}>Chung</span>}
          <NavLink 
            to="/dashboard" 
            className={({ isActive }) => `${styles.navItem} ${isActive ? styles.active : ''}`}
            onClick={onCloseMobile}
            title="Bảng điều khiển"
          >
            <FiGrid />
            {!isCollapsed && <span>Bảng điều khiển</span>}
          </NavLink>
        </div>

        <div className={styles.section}>
          <div className={styles.sectionHeader}>
            {!isCollapsed && <span className={styles.sectionTitle}>Không gian làm việc</span>}
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
          {!isCollapsed && <span className={styles.sectionTitle}>Khác</span>}
          <NavLink 
            to="/settings" 
            className={({ isActive }) => `${styles.navItem} ${isActive ? styles.active : ''}`}
            onClick={onCloseMobile}
            title="Cài đặt"
          >
            <FiSettings />
            {!isCollapsed && <span>Cài đặt</span>}
          </NavLink>
        </div>
      </nav>

      <div className={styles.footer}>
        {!isCollapsed && (
          <div className={styles.footerInfo}>
            <p className={styles.version}>phiên bản v1.0.0</p>
          </div>
        )}
      </div>
    </aside>
  );
};

export default Sidebar;
