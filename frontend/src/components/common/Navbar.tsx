import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { FiBell, FiLogOut, FiSettings, FiSearch } from 'react-icons/fi';
import { useAppDispatch, useAppSelector } from '../../hooks/useRedux';
import { logout } from '../../features/auth/authSlice';
import { apiSlice } from '../../services/apiSlice';
import { useGetUnreadCountQuery } from '../../services/notificationApi';
import NotificationDropdown from './NotificationDropdown';
import styles from './Navbar.module.css';

const Navbar: React.FC = () => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const location = useLocation();
  const user = useAppSelector((state) => state.auth.user);
  const [showNotifications, setShowNotifications] = useState(false);

  const { data: unreadCountRes } = useGetUnreadCountQuery(undefined, {
    pollingInterval: 5000,
  });

  const unreadCount = unreadCountRes?.data ?? 0;

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

  // Determine page title based on route
  const getPageTitle = () => {
    if (location.pathname === '/dashboard') return 'Dashboard';
    if (location.pathname.startsWith('/workspace/')) return 'Workspace';
    if (location.pathname.startsWith('/board/')) return 'Board View';
    if (location.pathname === '/settings') return 'Settings';
    return 'OKABE';
  };

  return (
    <nav className={styles.navbar}>
      <div className={styles.leftSection}>
        <h2 className={styles.pageTitle}>{getPageTitle()}</h2>
      </div>

      <div className={styles.rightSection}>
        <div className={styles.searchBox}>
          <FiSearch />
          <input type="text" placeholder="Global search..." disabled />
        </div>

        <div className={styles.navItem}>
          <button 
            className={styles.iconBtn} 
            onClick={(e) => {
              e.stopPropagation();
              setShowNotifications(!showNotifications);
            }}
          >
            <FiBell />
            {unreadCount > 0 && <span className={styles.badge}>{unreadCount > 99 ? '99+' : unreadCount}</span>}
          </button>
          {showNotifications && <NotificationDropdown onClose={() => setShowNotifications(false)} />}
        </div>

        <div className={styles.userSection}>
          <div className={styles.userInfo}>
            <div className={styles.avatar}>
              {user?.username?.charAt(0).toUpperCase()}
            </div>
            <span className={styles.username}>{user?.username}</span>
          </div>
          <div className={styles.divider}></div>
          <button onClick={handleLogout} className={styles.logoutBtn} title="Logout">
            <FiLogOut />
          </button>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
