import React, { useState } from 'react';
import { Board, useUpdateBoardVisibilityMutation } from '../../services/boardApi';
import { FiX, FiLink, FiGlobe, FiLock, FiCopy, FiCheck } from 'react-icons/fi';
import styles from './ShareBoardModal.module.css';

interface ShareBoardModalProps {
  board: Board;
  onClose: () => void;
}

const ShareBoardModal: React.FC<ShareBoardModalProps> = ({ board, onClose }) => {
  const [updateVisibility, { isLoading }] = useUpdateBoardVisibilityMutation();
  const [copied, setCopied] = useState(false);

  const publicUrl = `${window.location.origin}/public/${board.publicToken}`;

  const handleToggleVisibility = async () => {
    try {
      await updateVisibility({ id: board.id, isPublic: !board.isPublic }).unwrap();
    } catch (err) {
      alert('Không thể cập nhật trạng thái chia sẻ');
    }
  };

  const handleCopyLink = () => {
    navigator.clipboard.writeText(publicUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className={styles.overlay} onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className={styles.modal}>
        <header className={styles.header}>
          <h3>Chia sẻ bảng</h3>
          <button className={styles.closeBtn} onClick={onClose}><FiX /></button>
        </header>

        <div className={styles.body}>
          <div className={styles.section}>
            <div className={styles.sectionInfo}>
              <div className={styles.iconWrapper}>
                {board.isPublic ? <FiGlobe className={styles.publicIcon} /> : <FiLock className={styles.privateIcon} />}
              </div>
              <div className={styles.textWrapper}>
                <h4>Công khai bảng</h4>
                <p>
                  {board.isPublic 
                    ? 'Bất kỳ ai có liên kết đều có thể xem bảng này.' 
                    : 'Chỉ những thành viên được mời mới có thể xem bảng này.'}
                </p>
              </div>
              <label className={styles.switch}>
                <input 
                  type="checkbox" 
                  checked={board.isPublic} 
                  onChange={handleToggleVisibility}
                  disabled={isLoading}
                />
                <span className={styles.slider}></span>
              </label>
            </div>
          </div>

          {board.isPublic && (
            <div className={styles.linkSection}>
              <p className={styles.linkLabel}>Liên kết chia sẻ công khai</p>
              <div className={styles.linkInputGroup}>
                <div className={styles.linkIcon}><FiLink /></div>
                <input 
                  type="text" 
                  readOnly 
                  value={publicUrl} 
                  className={styles.linkInput}
                  onClick={(e) => (e.target as HTMLInputElement).select()}
                />
                <button className={styles.copyBtn} onClick={handleCopyLink}>
                  {copied ? <FiCheck style={{ color: '#22c55e' }} /> : <FiCopy />}
                  <span>{copied ? 'Đã chép' : 'Sao chép'}</span>
                </button>
              </div>
              <div className={styles.hint}>
                * Người xem ở chế độ công khai sẽ không thể chỉnh sửa nội dung.
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default ShareBoardModal;
