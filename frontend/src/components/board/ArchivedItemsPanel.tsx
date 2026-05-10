import React, { useState } from 'react';
import { 
  useGetArchivedListsQuery, 
  useGetArchivedCardsQuery, 
  useRestoreListMutation, 
  useRestoreCardMutation,
  useDeleteListMutation,
  useDeleteCardMutation
} from '../../services/boardApi';
import styles from './ArchivedItemsPanel.module.css';
import { FiX, FiRefreshCw, FiTrash2 } from 'react-icons/fi';

interface ArchivedItemsPanelProps {
  boardId: number;
  onClose: () => void;
}

const ArchivedItemsPanel: React.FC<ArchivedItemsPanelProps> = ({ boardId, onClose }) => {
  const [activeTab, setActiveTab] = useState<'lists' | 'cards'>('lists');
  const [cardPage, setCardPage] = useState(0);

  const { data: archivedListsRes, isLoading: loadingLists } = useGetArchivedListsQuery(boardId, {
    skip: activeTab !== 'lists'
  });
  const { data: archivedCardsRes, isLoading: loadingCards } = useGetArchivedCardsQuery({ 
    boardId, 
    page: cardPage,
    size: 20
  }, {
    skip: activeTab !== 'cards'
  });

  const [restoreList] = useRestoreListMutation();
  const [restoreCard] = useRestoreCardMutation();
  const [deleteList] = useDeleteListMutation();
  const [deleteCard] = useDeleteCardMutation();

  const handleRestoreList = async (listId: number) => {
    try {
      await restoreList({ id: listId, boardId }).unwrap();
    } catch (err: unknown) {
      const e = err as { data?: { message?: string } };
      alert(e.data?.message || 'Không thể khôi phục danh sách');
    }
  };

  const handleRestoreCard = async (cardId: number) => {
    try {
      await restoreCard({ id: cardId, boardId }).unwrap();
    } catch (err: unknown) {
      const e = err as { data?: { message?: string } };
      alert(e.data?.message || 'Không thể khôi phục thẻ');
    }
  };

  const handleDeleteList = async (listId: number) => {
    if (confirm('Xóa danh sách này vĩnh viễn? Hành động này không thể hoàn tác.')) {
      try {
        await deleteList({ id: listId, boardId }).unwrap();
      } catch (err: unknown) {
        const e = err as { data?: { message?: string } };
        alert(e.data?.message || 'Không thể xóa danh sách. Có thể bạn không có quyền.');
      }
    }
  };

  const handleDeleteCard = async (cardId: number) => {
    if (confirm('Xóa thẻ này vĩnh viễn? Hành động này không thể hoàn tác.')) {
      try {
        await deleteCard({ id: cardId, boardId }).unwrap();
      } catch (err: unknown) {
        const e = err as { data?: { message?: string } };
        alert(e.data?.message || 'Không thể xóa thẻ. Có thể bạn không có quyền.');
      }
    }
  };

  return (
    <div className={styles.panelOverlay} onClick={onClose}>
      <div className={styles.panel} onClick={e => e.stopPropagation()}>
        <div className={styles.header}>
          <h2>Mục đã lưu trữ</h2>
          <button className={styles.closeBtn} onClick={onClose}><FiX /></button>
        </div>

        <div className={styles.tabs}>
          <button 
            className={`${styles.tab} ${activeTab === 'lists' ? styles.active : ''}`}
            onClick={() => setActiveTab('lists')}
          >
            Danh sách
          </button>
          <button 
            className={`${styles.tab} ${activeTab === 'cards' ? styles.active : ''}`}
            onClick={() => setActiveTab('cards')}
          >
            Thẻ
          </button>
        </div>

        <div className={styles.content}>
          {activeTab === 'lists' ? (
            <div className={styles.listContainer}>
              {loadingLists ? (
                <div className={styles.loading}>Đang tải danh sách...</div>
              ) : archivedListsRes?.data.length === 0 ? (
                <div className={styles.empty}>Không có danh sách nào bị lưu trữ</div>
              ) : (
                archivedListsRes?.data.map(list => (
                  <div key={list.id} className={styles.item}>
                    <span className={styles.itemName}>{list.name}</span>
                    <div className={styles.actions}>
                      <button 
                        className={styles.actionBtn} 
                        onClick={() => handleRestoreList(list.id)}
                        title="Khôi phục"
                      >
                        <FiRefreshCw /> Khôi phục
                      </button>
                      <button 
                        className={`${styles.actionBtn} ${styles.danger}`} 
                        onClick={() => handleDeleteList(list.id)}
                        title="Xóa vĩnh viễn"
                      >
                        <FiTrash2 />
                      </button>
                    </div>
                  </div>
                ))
              )}
            </div>
          ) : (
            <div className={styles.listContainer}>
              {loadingCards ? (
                <div className={styles.loading}>Đang tải thẻ...</div>
              ) : archivedCardsRes?.data.content.length === 0 ? (
                <div className={styles.empty}>Không có thẻ nào bị lưu trữ</div>
              ) : (
                <>
                  {archivedCardsRes?.data.content.map(card => (
                    <div key={card.id} className={styles.item}>
                      <div className={styles.itemInfo}>
                        <span className={styles.itemName}>{card.title}</span>
                      </div>
                      <div className={styles.actions}>
                        <button 
                          className={styles.actionBtn} 
                          onClick={() => handleRestoreCard(card.id)}
                          title="Khôi phục"
                        >
                          <FiRefreshCw /> Khôi phục
                        </button>
                        <button 
                          className={`${styles.actionBtn} ${styles.danger}`} 
                          onClick={() => handleDeleteCard(card.id)}
                          title="Xóa vĩnh viễn"
                        >
                          <FiTrash2 />
                        </button>
                      </div>
                    </div>
                  ))}
                  {archivedCardsRes && archivedCardsRes.data.totalPages > 1 && (
                    <div className={styles.pagination}>
                      <button 
                        disabled={cardPage === 0} 
                        onClick={() => setCardPage(p => p - 1)}
                      >
                        Trước
                      </button>
                      <span>Trang {cardPage + 1} / {archivedCardsRes.data.totalPages}</span>
                      <button 
                        disabled={cardPage >= archivedCardsRes.data.totalPages - 1} 
                        onClick={() => setCardPage(p => p + 1)}
                      >
                        Sau
                      </button>
                    </div>
                  )}
                </>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default ArchivedItemsPanel;
