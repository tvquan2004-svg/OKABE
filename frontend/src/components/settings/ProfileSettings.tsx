import React, { useState, useEffect, useRef } from 'react';
import { FiCamera, FiCheck, FiMail, FiUser, FiShield, FiArrowRight, FiUploadCloud } from 'react-icons/fi';
import { useGetMeQuery, useUpdateProfileMutation, useUploadAvatarMutation } from '../../services/userApi';
import styles from './ProfileSettings.module.css';

interface ProfileSettingsProps {
  onNavigateToSecurity: () => void;
}

const ProfileSettings: React.FC<ProfileSettingsProps> = ({ onNavigateToSecurity }) => {
  const { data: user, isLoading } = useGetMeQuery();
  const [updateProfile, { isLoading: isUpdating }] = useUpdateProfileMutation();
  const [uploadAvatar, { isLoading: isUploading }] = useUploadAvatarMutation();

  const [username, setUsername] = useState('');
  const [avatarUrl, setAvatarUrl] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [error, setError] = useState('');
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (user) {
      setUsername(user.username);
      setAvatarUrl(user.avatarUrl || '');
    }
  }, [user]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!username.trim()) return;

    try {
      await updateProfile({
        username: username.trim(),
        avatarUrl: avatarUrl.trim() || undefined,
      }).unwrap();
      setSuccessMessage('Cập nhật hồ sơ thành công!');
      setTimeout(() => setSuccessMessage(''), 3000);
    } catch (err) {
      console.error('Failed to update profile:', err);
      setError('Cập nhật thất bại. Vui lòng thử lại.');
    }
  };

  const handleAvatarClick = () => {
    fileInputRef.current?.click();
  };

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // Check file size (max 5MB)
    if (file.size > 5 * 1024 * 1024) {
      setError('File quá lớn. Vui lòng chọn file dưới 5MB.');
      return;
    }

    const formData = new FormData();
    formData.append('file', file);

    try {
      setError('');
      await uploadAvatar(formData).unwrap();
      setSuccessMessage('Tải ảnh đại diện lên thành công!');
      setTimeout(() => setSuccessMessage(''), 3000);
    } catch (err) {
      console.error('Upload failed:', err);
      setError('Không thể tải ảnh lên. Vui lòng thử lại.');
    }
  };

  if (isLoading) return <div className={styles.loading}>Đang tải...</div>;

  return (
    <div className={styles.container}>
      <div className={styles.profileHeader}>
        <div className={styles.banner}>
          <div className={styles.bannerOverlay}></div>
        </div>
        <div className={styles.avatarSection}>
          <div className={styles.avatarWrapper}>
            <img 
              src={avatarUrl || `https://ui-avatars.com/api/?name=${username}&background=6366f1&color=fff`} 
              alt={username} 
              className={`${styles.avatar} ${isUploading ? styles.avatarLoading : ''}`}
            />
            <button 
              className={styles.cameraBtn} 
              onClick={handleAvatarClick}
              disabled={isUploading}
              title="Tải ảnh lên từ máy"
            >
              {isUploading ? <div className={styles.spinner}></div> : <FiCamera />}
            </button>
            <input 
              type="file" 
              ref={fileInputRef} 
              className={styles.hiddenInput} 
              accept="image/*"
              onChange={handleFileChange}
            />
          </div>
          <div className={styles.profileTitle}>
            <h2>{user?.username}</h2>
            <p>{user?.email}</p>
          </div>
        </div>
      </div>

      <form className={styles.form} onSubmit={handleSubmit}>
        <div className={styles.formGrid}>
          <div className={styles.fieldGroup}>
            <label className={styles.label}>
              <FiUser /> Tên người dùng
            </label>
            <input 
              type="text" 
              className={styles.input}
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="Nhập tên của bạn"
            />
          </div>

          <div className={styles.fieldGroup}>
            <label className={styles.label}>
              <FiMail /> Địa chỉ Email
            </label>
            <input 
              type="email" 
              className={`${styles.input} ${styles.readonly}`}
              value={user?.email}
              readOnly
              disabled
            />
            <span className={styles.inputHint}>Email không thể thay đổi</span>
          </div>

          <div className={styles.fieldGroup}>
            <label className={styles.label}>
              <FiUploadCloud /> URL ảnh (Tùy chọn)
            </label>
            <input 
              type="text" 
              className={styles.input}
              value={avatarUrl}
              onChange={(e) => setAvatarUrl(e.target.value)}
              placeholder="Hoặc dán URL ảnh tại đây"
            />
          </div>

          <div className={styles.fieldGroup}>
            <label className={styles.label}>
              <FiShield /> Bảo mật nâng cao
            </label>
            <div className={styles.securityBox} onClick={onNavigateToSecurity}>
              <div className={styles.securityInfo}>
                <span className={user?.is2faEnabled ? styles.statusEnabled : styles.statusDisabled}>
                  {user?.is2faEnabled ? 'Đã bật 2FA' : 'Chưa bật 2FA'}
                </span>
                <p>Thiết lập xác thực 2 lớp để bảo vệ tài khoản</p>
              </div>
              <FiArrowRight className={styles.arrowIcon} />
            </div>
          </div>
        </div>

        <div className={styles.footer}>
          {error && <span className={styles.errorMsg}>{error}</span>}
          {successMessage && <span className={styles.successMsg}>{successMessage}</span>}
          <button 
            type="submit" 
            className="btn btn-primary"
            disabled={isUpdating || !username.trim()}
          >
            {isUpdating ? 'Đang lưu...' : 'Lưu thay đổi'}
          </button>
        </div>
      </form>
    </div>
  );
};

export default ProfileSettings;
