import React from 'react';
import NotificationSettings from '../components/settings/NotificationSettings';
import styles from './SettingsPage.module.css';

const SettingsPage: React.FC = () => {
  return (
    <div className={styles.pageContainer}>
      <main className={styles.mainContent}>
        <div className={styles.sidebar}>
          <h2 className={styles.sidebarTitle}>User Settings</h2>
          <nav className={styles.sideNav}>
            <button className={`${styles.navItem} ${styles.active}`}>
              <span>🔔</span> Notifications
            </button>
            <button className={styles.navItem} disabled>
              <span>👤</span> Profile (Coming Soon)
            </button>
            <button className={styles.navItem} disabled>
              <span>🔒</span> Security (Coming Soon)
            </button>
          </nav>
        </div>
        
        <div className={styles.contentArea}>
          <NotificationSettings />
        </div>
      </main>
    </div>
  );
};

export default SettingsPage;
