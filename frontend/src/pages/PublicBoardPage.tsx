import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useGetPublicBoardQuery } from '../services/boardApi';
import BoardListColumn from '../components/board/BoardListColumn';
import CardDetailModal from '../components/board/CardDetailModal';
import { FiExternalLink, FiLock, FiInfo } from 'react-icons/fi';
import styles from './BoardPage.module.css';
import publicStyles from './PublicBoardPage.module.css';

function PublicBoardPage() {
  const { token } = useParams<{ token: string }>();
  const { data: boardData, isLoading, error } = useGetPublicBoardQuery(token || '', {
    skip: !token
  });

  const [selectedCard, setSelectedCard] = useState<any>(null);

  useEffect(() => {
    if (boardData?.data?.name) {
      document.title = `${boardData.data.name} - OKABE`;
    }
  }, [boardData]);

  if (isLoading) return <div className={styles.loading}>Đang tải bảng công khai...</div>;
  
  if (error || !boardData?.data) {
    return (
      <div className={publicStyles.errorContainer}>
        <FiLock size={48} className={publicStyles.errorIcon} />
        <h2>Bảng không tồn tại hoặc đã bị gỡ bỏ chế độ công khai</h2>
        <p>Vui lòng kiểm tra lại đường dẫn hoặc liên hệ với chủ sở hữu bảng.</p>
        <Link to="/" className="btn btn-primary">Quay lại trang chủ</Link>
      </div>
    );
  }

  const board = boardData.data;
  const lists = board.lists ?? [];

  const priorityColor = (priority: string) => {
    switch (priority) {
      case 'CRITICAL': return '#ef4444';
      case 'HIGH': return '#f59e0b';
      case 'MEDIUM': return '#3b82f6';
      case 'LOW': return '#22c55e';
      default: return '#64748b';
    }
  };

  const isImageUrl = board.background?.startsWith('http') || board.background?.startsWith('/api/v1/files/');

  const containerStyle: React.CSSProperties = {
    backgroundImage: isImageUrl 
      ? `linear-gradient(rgba(0, 0, 0, 0.4), rgba(0, 0, 0, 0.4)), url(${board.background})` 
      : 'none',
    backgroundColor: board.background?.startsWith('#') ? board.background : '#1a1a2e',
    backgroundSize: 'cover',
    backgroundPosition: 'center',
    backgroundRepeat: 'no-repeat',
    backgroundAttachment: 'fixed',
    minHeight: '100vh',
  };

  return (
    <div className={styles.container} style={containerStyle}>
      <div className={publicStyles.publicBanner}>
        <div className={publicStyles.bannerContent}>
          <FiInfo className={publicStyles.bannerIcon} />
          <span>Bạn đang xem bảng ở chế độ công khai. <strong>Đăng nhập</strong> để có thể chỉnh sửa và cộng tác.</span>
          <Link to="/login" className={publicStyles.bannerLink}>
            Đăng nhập ngay <FiExternalLink />
          </Link>
        </div>
      </div>

      <header className={styles.header}>
        <div className={styles.boardMeta}>
          <h1 style={{ color: 'white', margin: 0, fontSize: '1.5rem' }}>{board.name}</h1>
          {board.description && <span className={styles.boardDesc} style={{ opacity: 0.8 }}>{board.description}</span>}
        </div>
        <div className={styles.boardActions}>
          <div className={publicStyles.publicBadge}>CÔNG KHAI</div>
        </div>
      </header>

      <div className={styles.kanban} style={{ marginTop: '20px' }}>
        {lists.map((list) => (
          <BoardListColumn
            key={list.id}
            list={list}
            onEditList={() => {}}
            onDeleteList={() => {}}
            onArchiveList={() => {}}
            onAddCard={async () => {}}
            onCardClick={setSelectedCard}
            priorityColor={priorityColor}
            matchedCardIds={null}
            readOnly={true}
          />
        ))}
      </div>

      {selectedCard && (
        <CardDetailModal
          card={selectedCard}
          boardId={board.id}
          workspaceId={0} // Not needed for public view as mutations are disabled
          onClose={() => setSelectedCard(null)}
          priorityColor={priorityColor}
          readOnly={true}
        />
      )}
    </div>
  );
}

export default PublicBoardPage;
