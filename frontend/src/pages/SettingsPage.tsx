import React from 'react';
import { FiBell, FiLock, FiUser, FiArrowLeft, FiChevronRight } from 'react-icons/fi';
import { useNavigate } from 'react-router-dom';
import NotificationSettings from '../components/settings/NotificationSettings';
import SecuritySettingsPanel from '../components/settings/SecuritySettingsPanel';
import ProfileSettings from '../components/settings/ProfileSettings';
import styles from './SettingsPage.module.css';

const SettingsPage: React.FC = () => {
  const [activeTab, setActiveTab] = React.useState<'notifications' | 'security' | 'profile'>('profile');
  const navigate = useNavigate();

  return (
    <div className={styles.pageContainer}>
      <header className={styles.header}>
        <div className={styles.headerContent}>
          <button className={styles.backBtn} onClick={() => navigate(-1)}>
            <FiArrowLeft /> Quay lại
          </button>
          <div className={styles.titleSection}>
            <h1 className={styles.pageTitle}>Cài đặt hệ thống</h1>
            <p className={styles.pageSubtitle}>Quản lý tài khoản, bảo mật và thông báo của bạn</p>
          </div>
        </div>
      </header>

      <main className={styles.mainContent}>
        <aside className={styles.sidebar}>
          <nav className={styles.sideNav}>
            <button 
              className={`${styles.navItem} ${activeTab === 'notifications' ? styles.active : ''}`}
              onClick={() => setActiveTab('notifications')}
            >
              <div className={styles.iconWrapper}><FiBell /></div>
              <div className={styles.navLabel}>
                <span className={styles.navTitle}>Thông báo</span>
                <span className={styles.navDesc}>Tùy chỉnh cách bạn nhận tin</span>
              </div>
              <FiChevronRight className={styles.arrowIcon} />
            </button>

            <button 
              className={`${styles.navItem} ${activeTab === 'security' ? styles.active : ''}`}
              onClick={() => setActiveTab('security')}
            >
              <div className={styles.iconWrapper}><FiLock /></div>
              <div className={styles.navLabel}>
                <span className={styles.navTitle}>Bảo mật</span>
                <span className={styles.navDesc}>Xác thực 2 lớp và mật khẩu</span>
              </div>
              <FiChevronRight className={styles.arrowIcon} />
            </button>

            <button 
              className={`${styles.navItem} ${activeTab === 'profile' ? styles.active : ''}`}
              onClick={() => setActiveTab('profile')}
            >
              <div className={styles.iconWrapper}><FiUser /></div>
              <div className={styles.navLabel}>
                <span className={styles.navTitle}>Hồ sơ</span>
                <span className={styles.navDesc}>Thông tin cá nhân</span>
              </div>
              <FiChevronRight className={styles.arrowIcon} />
            </button>
          </nav>
        </aside>
        
        <div className={styles.contentArea}>
          <div className={styles.contentCard}>
            {activeTab === 'notifications' && <NotificationSettings />}
            {activeTab === 'security' && <SecuritySettingsPanel />}
            {activeTab === 'profile' && <ProfileSettings onNavigateToSecurity={() => setActiveTab('security')} />}
          </div>
        </div>
      </main>
    </div>
  );
};

export default SettingsPage;
