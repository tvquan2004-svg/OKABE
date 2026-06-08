import { useState, useEffect } from 'react';
import { useGetStandupQuery } from '../../services/aiApi';
import styles from './StandupModal.module.css';

interface StandupModalProps {
  workspaceId: number;
  onClose: () => void;
  onSendToWorkspace?: (text: string) => void;
}

const StandupModal: React.FC<StandupModalProps> = ({ workspaceId, onClose, onSendToWorkspace }) => {
  const today = new Date().toISOString().slice(0, 10);
  const [selectedDate, setSelectedDate] = useState(today);
  const { data: standupRes, isLoading, error } = useGetStandupQuery({
    workspaceId,
    date: selectedDate,
  });

  const standup = standupRes?.data;

  const [done, setDone] = useState('');
  const [inProgress, setInProgress] = useState('');
  const [blocked, setBlocked] = useState('');
  const [isEditing, setIsEditing] = useState(false);
  const [sending, setSending] = useState(false);

  useEffect(() => {
    if (standup) {
      try {
        const parsed = JSON.parse(standup.done);
        setDone(parsed.done || standup.done);
        setInProgress(parsed.inProgress || standup.inProgress);
        setBlocked(parsed.blocked || standup.blocked);
      } catch {
        setDone(standup.done);
        setInProgress(standup.inProgress);
        setBlocked(standup.blocked);
      }
    }
  }, [standup]);

  const handleSend = async () => {
    if (!onSendToWorkspace) return;
    setSending(true);
    const text = `📋 **Tổng kết ngày ${selectedDate}**\n\n` +
      `**Đã làm:**\n${done}\n\n` +
      `**Đang làm:**\n${inProgress}\n\n` +
      `**Cần hỗ trợ:**\n${blocked}`;
    await onSendToWorkspace(text);
    setSending(false);
    onClose();
  };

  return (
    <div className={styles.overlay} onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className={styles.modal}>
        <div className={styles.header}>
          <h2 className={styles.title}>Tổng kết ngày</h2>
          <button className={styles.closeBtn} onClick={onClose}>&times;</button>
        </div>

        <div className={styles.body}>
          <div className={styles.dateRow}>
            <input
              type="date"
              value={selectedDate}
              onChange={(e) => setSelectedDate(e.target.value)}
              className={styles.dateInput}
            />
          </div>

          {isLoading ? (
            <div className={styles.statusText}>Đang tổng hợp hoạt động...</div>
          ) : error ? (
            <div className={`${styles.statusText} ${styles.error}`}>Không thể tạo tổng kết. Vui lòng thử lại.</div>
          ) : !standup ? (
            <div className={styles.statusText}>Không có dữ liệu cho ngày này.</div>
          ) : (
            <>
              {standup.userName && (
                <p className={styles.userInfo}>{standup.userName}</p>
              )}

              <div className={styles.section}>
                <h4 className={`${styles.sectionTitle} ${styles.done}`}>Đã làm</h4>
                {isEditing ? (
                  <textarea
                    className={styles.editArea}
                    value={done}
                    onChange={(e) => setDone(e.target.value)}
                  />
                ) : (
                  <div className={styles.content}>{done}</div>
                )}
              </div>

              <div className={styles.section}>
                <h4 className={`${styles.sectionTitle} ${styles.inProgress}`}>Đang làm</h4>
                {isEditing ? (
                  <textarea
                    className={styles.editArea}
                    value={inProgress}
                    onChange={(e) => setInProgress(e.target.value)}
                  />
                ) : (
                  <div className={styles.content}>{inProgress}</div>
                )}
              </div>

              <div className={styles.section}>
                <h4 className={`${styles.sectionTitle} ${styles.blocked}`}>Cần hỗ trợ</h4>
                {isEditing ? (
                  <textarea
                    className={styles.editArea}
                    value={blocked}
                    onChange={(e) => setBlocked(e.target.value)}
                  />
                ) : (
                  <div className={styles.content}>{blocked}</div>
                )}
              </div>
            </>
          )}
        </div>

        <div className={styles.footer}>
          <button className={styles.editBtn} onClick={() => setIsEditing(!isEditing)}>
            {isEditing ? 'Xong chỉnh sửa' : 'Chỉnh sửa'}
          </button>
          {onSendToWorkspace && (
            <button
              className={styles.sendBtn}
              onClick={handleSend}
              disabled={isLoading || sending}
            >
              {sending ? 'Đang gửi...' : 'Gửi lên workspace'}
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

export default StandupModal;
