import React from 'react';
import { useGetNotificationPreferencesQuery, useUpdateNotificationPreferencesMutation } from '../../services/userApi';
import styles from './NotificationSettings.module.css';

const NotificationSettings: React.FC = () => {
  const { data: preferences, isLoading } = useGetNotificationPreferencesQuery();
  const [updatePreferences, { isLoading: isUpdating }] = useUpdateNotificationPreferencesMutation();

  if (isLoading) return <div>Đang tải cài đặt...</div>;

  const handleToggle = async (key: keyof typeof preferences) => {
    if (!preferences) return;
    
    const newPreferences = {
      ...preferences,
      [key]: !preferences[key],
    };

    try {
      await updatePreferences(newPreferences).unwrap();
    } catch (error) {
      console.error('Failed to update preferences:', error);
    }
  };

  const settings = [
    {
      id: 'emailAssigned',
      title: 'Giao việc cho bạn',
      description: 'Nhận email khi có ai đó giao thẻ công việc cho bạn.',
      icon: '👤',
    },
    {
      id: 'emailMentioned',
      title: 'Nhắc tên bạn',
      description: 'Nhận email khi có ai đó nhắc tên bạn trong bình luận.',
      icon: '💬',
    },
    {
      id: 'emailDueSoon',
      title: 'Nhắc nhở hạn chót',
      description: 'Nhận email nhắc nhở trước 24 giờ khi thẻ sắp đến hạn.',
      icon: '⏰',
    },
    {
      id: 'emailInvited',
      title: 'Lời mời vào Không gian',
      description: 'Nhận email khi bạn được mời vào một không gian làm việc mới.',
      icon: '📩',
    },
  ];

  return (
    <div className={styles.container}>
      <h2 className={styles.title}>Thông báo qua Email</h2>
      <p className={styles.subtitle}>Chọn các sự kiện bạn muốn nhận thông báo qua email.</p>
      
      <div className={styles.settingsList}>
        {settings.map((item) => (
          <div key={item.id} className={styles.settingItem}>
            <div className={styles.itemInfo}>
              <span className={styles.itemIcon}>{item.icon}</span>
              <div>
                <h4 className={styles.itemTitle}>{item.title}</h4>
                <p className={styles.itemDescription}>{item.description}</p>
              </div>
            </div>
            <label className={styles.switch}>
              <input
                type="checkbox"
                checked={preferences?.[item.id as keyof typeof preferences] || false}
                onChange={() => handleToggle(item.id as keyof typeof preferences)}
                disabled={isUpdating}
              />
              <span className={styles.slider}></span>
            </label>
          </div>
        ))}
      </div>
    </div>
  );
};

export default NotificationSettings;
