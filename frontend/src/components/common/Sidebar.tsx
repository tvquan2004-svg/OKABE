import React, { useState } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import { 
  FiLayout, 
  FiX
} from 'react-icons/fi';
import { useGetWorkspacesQuery } from '../../services/workspaceApi';
import { useGetBoardQuery } from '../../services/boardApi';
import SuggestionPanel from './SuggestionPanel';
import styles from './Sidebar.module.css';

interface SidebarProps {
  isCollapsed: boolean;
  onToggle: () => void;
  isMobileOpen?: boolean;
  onCloseMobile?: () => void;
}

const Sidebar: React.FC<SidebarProps> = ({ isCollapsed, onToggle, isMobileOpen, onCloseMobile }) => {
  const location = useLocation();
  const { data: workspacesRes } = useGetWorkspacesQuery();
  const workspaces = workspacesRes?.data ?? [];
  const [tooltip, setTooltip] = useState<{ text: string; rect: DOMRect } | null>(null);

  const boardIdMatch = location.pathname.match(/\/board\/(\d+)/);
  const boardId = boardIdMatch ? Number(boardIdMatch[1]) : null;
  const { data: boardData } = useGetBoardQuery(boardId as number, { skip: !boardId });
  const workspaceIdMatch = location.pathname.match(/\/workspace\/(\d+)/);
  const currentWorkspaceId = workspaceIdMatch
    ? Number(workspaceIdMatch[1])
    : boardData?.data?.workspaceId ?? null;

  const handleSidebarClick = (e: React.MouseEvent) => {
    const target = e.target as HTMLElement;
    if (!target.closest('a, button, [role="button"]')) {
      onToggle();
    }
  };

  const showTooltip = (e: React.MouseEvent, text: string) => {
    if (!isCollapsed) return;
    setTooltip({ text, rect: (e.currentTarget as HTMLElement).getBoundingClientRect() });
  };

  const hideTooltip = () => setTooltip(null);

  return (
    <aside className={`
      ${styles.sidebar} 
      ${isCollapsed ? styles.collapsed : ''} 
      ${isMobileOpen ? styles.mobileOpen : ''}
    `} onClick={handleSidebarClick}>
      <div className={styles.header}>
        <div className={styles.logoContainer}>
          <img src="/favicon.png" className={styles.logoImg} alt="Logo" />
          {!isCollapsed && <span className={styles.logoText}>OKABE</span>}
        </div>
        <div className={styles.headerActions}>
          <button className={styles.closeMobileBtn} onClick={(e) => { e.stopPropagation(); onCloseMobile?.(); }}>
            <FiX />
          </button>
        </div>
      </div>

      <nav className={styles.nav}>
        <NavLink 
          to="/dashboard" 
          className={({ isActive }) => `${styles.navItem} ${isActive ? styles.active : ''}`}
          onClick={onCloseMobile}
          onMouseEnter={(e) => showTooltip(e, 'Bảng điều khiển')}
          onMouseLeave={hideTooltip}
        >
          <FiLayout />
          {!isCollapsed && <span>Bảng điều khiển</span>}
        </NavLink>

        <div className={styles.section}>
          {workspaces.map(ws => (
            <NavLink 
              key={ws.id}
              to={`/workspace/${ws.id}`}
              className={({ isActive }) => `${styles.navItem} ${isActive ? styles.active : ''}`}
              onClick={onCloseMobile}
              onMouseEnter={(e) => showTooltip(e, ws.name)}
              onMouseLeave={hideTooltip}
            >
              <div className={styles.wsIcon}>{ws.name.charAt(0).toUpperCase()}</div>
              {!isCollapsed && <span className={styles.wsName}>{ws.name}</span>}
            </NavLink>
          ))}
        </div>

        {!isCollapsed && <SuggestionPanel workspaceId={currentWorkspaceId} />}
      </nav>

      {tooltip && (
        <div style={{
          position: 'fixed',
          left: tooltip.rect.right + 10,
          top: tooltip.rect.top + tooltip.rect.height / 2,
          transform: 'translateY(-50%)',
          background: '#1a1a1a',
          color: '#fff',
          padding: '5px 11px',
          borderRadius: 6,
          fontSize: '0.75rem',
          fontWeight: 500,
          whiteSpace: 'nowrap',
          zIndex: 9999,
          pointerEvents: 'none',
          boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
          letterSpacing: '-0.01em',
          lineHeight: 1.3,
        }}>
          {tooltip.text}
        </div>
      )}
    </aside>
  );
};

export default Sidebar;
