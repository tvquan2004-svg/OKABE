import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { FiBell, FiLogOut } from 'react-icons/fi';
import { useAppDispatch, useAppSelector } from '../../hooks/useRedux';
import { logout } from '../../features/auth/authSlice';
import { apiSlice } from '../../services/apiSlice';
import { useGetUnreadCountQuery } from '../../services/notificationApi';
import NotificationDropdown from './NotificationDropdown';
import styles from './Navbar.module.css';

const Navbar: React.FC = () => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const user = useAppSelector((state) => state.auth.user);
  const [showNotifications, setShowNotifications] = useState(false);

  const { data: unreadCountRes } = useGetUnreadCountQuery(undefined, {
    pollingInterval: 5000, // Poll every 5s for better responsiveness during test
  });

  const unreadCount = unreadCountRes?.data ?? 0;

  const handleLogout = () => {
    dispatch(logout());
    dispatch(apiSlice.util.resetApiState());
    navigate('/login');
  };

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = () => setShowNotifications(false);
    if (showNotifications) {
      window.addEventListener('click', handleClickOutside);
    }
    return () => window.removeEventListener('click', handleClickOutside);
  }, [showNotifications]);

  return (
    <nav className={styles.navbar}>
      <div className={styles.leftSection}>
        <Link to="/dashboard" className={styles.logo}>
          <span className={styles.logoIcon}>⚡</span>
          <span className={styles.logoText}>OKABE</span>
        </Link>
      </div>

      <div className={styles.rightSection}>
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

        <div className={styles.userProfile}>
          <div className={styles.avatar}>
            {user?.username?.charAt(0).toUpperCase()}
          </div>
          <span className={styles.username}>{user?.username}</span>
          <button onClick={handleLogout} className={styles.logoutBtn}>
            <FiLogOut style={{ marginRight: '6px' }} />
            Logout
          </button>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
