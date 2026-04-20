import React from 'react';
import { useGetNotificationPreferencesQuery, useUpdateNotificationPreferencesMutation } from '../../services/userApi';
import styles from './NotificationSettings.module.css';

const NotificationSettings: React.FC = () => {
  const { data: preferences, isLoading } = useGetNotificationPreferencesQuery();
  const [updatePreferences, { isLoading: isUpdating }] = useUpdateNotificationPreferencesMutation();

  if (isLoading) return <div>Loading preferences...</div>;

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
      title: 'Card Assignments',
      description: 'Receive an email when someone assigns you to a card.',
      icon: '👤',
    },
    {
      id: 'emailMentioned',
      title: 'Mentions',
      description: 'Receive an email when someone mentions you in a comment.',
      icon: '💬',
    },
    {
      id: 'emailDueSoon',
      title: 'Due Soon Reminders',
      description: 'Receive a reminder email 24 hours before a card is due.',
      icon: '⏰',
    },
    {
      id: 'emailInvited',
      title: 'Workspace Invitations',
      description: 'Receive an email when you are invited to a new workspace.',
      icon: '📩',
    },
  ];

  return (
    <div className={styles.container}>
      <h2 className={styles.title}>Email Notifications</h2>
      <p className={styles.subtitle}>Choose which events you want to be notified about via email.</p>
      
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
