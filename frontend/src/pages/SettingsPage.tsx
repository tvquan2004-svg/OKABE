import React from 'react';
import NotificationSettings from '../components/settings/NotificationSettings';
import SecuritySettingsPanel from '../components/settings/SecuritySettingsPanel';
import styles from './SettingsPage.module.css';

const SettingsPage: React.FC = () => {
  const [activeTab, setActiveTab] = React.useState<'notifications' | 'security'>('notifications');

  return (
    <div className={styles.pageContainer}>
      <main className={styles.mainContent}>
        <div className={styles.sidebar}>
          <h2 className={styles.sidebarTitle}>Cài đặt người dùng</h2>
          <nav className={styles.sideNav}>
            <button 
              className={`${styles.navItem} ${activeTab === 'notifications' ? styles.active : ''}`}
              onClick={() => setActiveTab('notifications')}
            >
              <span>🔔</span> Thông báo
            </button>
            <button 
              className={`${styles.navItem} ${activeTab === 'security' ? styles.active : ''}`}
              onClick={() => setActiveTab('security')}
            >
              <span>🔒</span> Bảo mật
            </button>
            <button className={styles.navItem} disabled title="Sắp ra mắt">
              <span>👤</span> Hồ sơ
            </button>
          </nav>
        </div>
        
        <div className={styles.contentArea}>
          {activeTab === 'notifications' ? <NotificationSettings /> : <SecuritySettingsPanel />}
        </div>
      </main>
    </div>
  );
};

export default SettingsPage;
