import React, { useState, useEffect, useRef } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { FiBell, FiLogOut, FiMenu } from 'react-icons/fi';
import { useAppDispatch, useAppSelector } from '../../hooks/useRedux';
import { logout } from '../../features/auth/authSlice';
import { apiSlice } from '../../services/apiSlice';
import { useGetBoardQuery } from '../../services/boardApi';
import { useGetUnreadCountQuery } from '../../services/notificationApi';
import { useGetMeQuery } from '../../services/userApi';
import { useGetWorkspacesQuery } from '../../services/workspaceApi';
import NotificationDropdown from './NotificationDropdown';
import StandupModal from './StandupModal';
import styles from './Navbar.module.css';

interface NavbarProps {
  onMenuToggle: () => void;
}

const Navbar: React.FC<NavbarProps> = ({ onMenuToggle }) => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const location = useLocation();
  const { data: userData } = useGetMeQuery();
  const authUser = useAppSelector((state) => state.auth.user);
  const user = userData || authUser;
  const [showNotifications, setShowNotifications] = useState(false);
  const [showStandup, setShowStandup] = useState(false);
  const bellBtnRef = useRef<HTMLButtonElement>(null);
  const [notifPos, setNotifPos] = useState<{ top: number; right: number } | null>(null);

  const { data: unreadCountRes } = useGetUnreadCountQuery(undefined, {
    pollingInterval: 5000,
  });

  const unreadCount = unreadCountRes?.data ?? 0;

  const { data: workspacesRes } = useGetWorkspacesQuery();
  const workspaces = workspacesRes?.data || [];

  const handleLogout = () => {
    dispatch(logout());
    dispatch(apiSlice.util.resetApiState());
    navigate('/login');
  };

  useEffect(() => {
    const handleClickOutside = () => setShowNotifications(false);
    if (showNotifications) {
      window.addEventListener('click', handleClickOutside);
    }
    return () => window.removeEventListener('click', handleClickOutside);
  }, [showNotifications]);

  const boardIdMatch = location.pathname.match(/\/board\/(\d+)/);
  const boardId = boardIdMatch ? Number(boardIdMatch[1]) : null;
  const { data: boardData } = useGetBoardQuery(boardId as number, { skip: !boardId });
  const workspaceIdMatch = location.pathname.match(/\/workspace\/(\d+)/);
  const currentWorkspaceId = workspaceIdMatch
    ? Number(workspaceIdMatch[1])
    : boardData?.data.workspaceId || workspaces[0]?.id;

  const getPageTitle = () => {
    if (location.pathname === '/dashboard') return 'Bảng điều khiển';
    if (location.pathname.startsWith('/workspace/')) return 'Không gian làm việc';
    if (location.pathname.startsWith('/board/')) {
      return boardData?.data.name || 'Bảng công việc';
    }
    if (location.pathname === '/settings') return 'Cài đặt';
    return 'OKABE';
  };

  // 5PM reminder — show notification badge if after 5PM and standup not viewed today
  const todayStr = new Date().toISOString().slice(0, 10);
  const currentHour = new Date().getHours();
  const standupSeenToday = localStorage.getItem('standup_seen') === todayStr;
  const showStandupReminder = currentHour >= 17 && !standupSeenToday;

  const handleOpenStandup = () => {
    localStorage.setItem('standup_seen', todayStr);
    setShowStandup(true);
  };

  return (
    <>
    <nav className={styles.navbar}>
      <div className={styles.leftSection}>
        <button className={styles.menuBtn} onClick={onMenuToggle}>
          <FiMenu />
        </button>
        <h2 className={styles.pageTitle}>{getPageTitle()}</h2>
      </div>

      <div className={styles.rightSection}>
        <div className={styles.navItem}>
          <button className={styles.iconBtn} onClick={handleOpenStandup} title="Tổng kết ngày">
            📋
            {showStandupReminder && <span className={styles.badge}>!</span>}
          </button>
        </div>

        <div className={styles.navItem}>
          <button 
            ref={bellBtnRef}
            className={styles.iconBtn} 
            onClick={(e) => {
              e.stopPropagation();
              if (bellBtnRef.current) {
                const rect = bellBtnRef.current.getBoundingClientRect();
                setNotifPos({
                  top: rect.bottom + 12,
                  right: window.innerWidth - rect.right,
                });
              }
              setShowNotifications(prev => !prev);
            }}
          >
            <FiBell />
            {unreadCount > 0 && <span className={styles.badge}>{unreadCount > 99 ? '99+' : unreadCount}</span>}
          </button>
        </div>

        <div className={styles.userSection}>
          <div className={styles.userInfo}>
            <div className={styles.avatar}>
              {user?.avatarUrl ? (
                <img src={user.avatarUrl} alt={user.username} className={styles.avatarImg} />
              ) : (
                user?.username?.charAt(0).toUpperCase()
              )}
            </div>
            <span className={styles.username}>{user?.username}</span>
          </div>
          <div className={styles.divider}></div>
          <button onClick={handleLogout} className={styles.logoutBtn} title="Đăng xuất">
            <FiLogOut />
          </button>
        </div>
      </div>
    </nav>

      {showNotifications && notifPos && (
        <NotificationDropdown
          onClose={() => setShowNotifications(false)}
          position={notifPos}
        />
      )}

      {showStandup && currentWorkspaceId && (
        <StandupModal
          workspaceId={currentWorkspaceId}
          onClose={() => setShowStandup(false)}
        />
      )}
    </>
  );
};

export default Navbar;
