import React, { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useLinkCardsToKeyResultMutation } from '../../services/okrApi';
import { useGetWorkspaceCardsQuery, type CardSelection } from '../../services/boardApi';
import { type KeyResult } from '../../services/okrApi';
import styles from './KeyResultRow.module.css';

interface KeyResultRowProps {
  kr: KeyResult;
  objectiveId: number;
  workspaceId?: number;
}

const KeyResultRow: React.FC<KeyResultRowProps> = ({ kr, workspaceId }) => {
  const navigate = useNavigate();
  const [linkCards] = useLinkCardsToKeyResultMutation();
  const [showLinkModal, setShowLinkModal] = useState(false);
  const { data: cardsRes } = useGetWorkspaceCardsQuery(workspaceId!, { skip: !workspaceId || kr.linkedCards.length === 0 });

  const cardBoardMap = useMemo(() => {
    const map = new Map<number, number>();
    if (cardsRes?.data) {
      for (const c of cardsRes.data) {
        map.set(c.id, c.boardId);
      }
    }
    return map;
  }, [cardsRes]);

  const progressPct = kr.targetValue && kr.targetValue > 0
    ? Math.min(100, Math.round((kr.currentValue / kr.targetValue) * 100))
    : 0;

  return (
    <div className={styles.kr}>
      <div className={styles.krHeader}>
        <span className={styles.krTitle}>{kr.title}</span>
        <button className={styles.linkBtn} onClick={() => setShowLinkModal(true)}>
          + Thẻ
        </button>
      </div>

      <div className={styles.progressRow}>
        <div className={styles.progressTrack}>
          <div className={styles.progressFill} style={{ width: `${progressPct}%` }} />
        </div>
        <span className={styles.progressLabel}>
          {kr.currentValue}/{kr.targetValue ?? '?'} {kr.unit === 'percent' ? '%' : kr.unit}
        </span>
      </div>

      {kr.linkedCards.length > 0 && (
        <div className={styles.linkedCards}>
          {kr.linkedCards.map((cardId) => (
            <span
              key={cardId}
              className={styles.cardChip}
              onClick={() => {
                const bid = cardBoardMap.get(cardId);
                if (bid) {
                  navigate(`/board/${bid}?cardId=${cardId}`);
                }
              }}
              title={`Mở thẻ #${cardId}`}
            >
              #{cardId}
            </span>
          ))}
        </div>
      )}

      {showLinkModal && workspaceId && (
        <LinkCardModal
          workspaceId={workspaceId}
          selectedIds={kr.linkedCards}
          onConfirm={(ids) => {
            linkCards({ keyResultId: kr.id, cardIds: ids });
            setShowLinkModal(false);
          }}
          onClose={() => setShowLinkModal(false)}
        />
      )}
    </div>
  );
};

const LinkCardModal: React.FC<{
  workspaceId: number;
  selectedIds: number[];
  onConfirm: (ids: number[]) => void;
  onClose: () => void;
}> = ({ workspaceId, selectedIds, onConfirm, onClose }) => {
  const { data: cardsRes } = useGetWorkspaceCardsQuery(workspaceId);
  const cards = cardsRes?.data ?? [];
  const [search, setSearch] = useState('');
  const [selected, setSelected] = useState<Set<number>>(new Set(selectedIds));

  const filtered = useMemo(
    () => cards.filter(c => c.title.toLowerCase().includes(search.toLowerCase()) || String(c.id).includes(search)),
    [cards, search],
  );

  const grouped = useMemo(() => {
    const map = new Map<string, CardSelection[]>();
    for (const c of filtered) {
      const key = c.boardName;
      if (!map.has(key)) map.set(key, []);
      map.get(key)!.push(c);
    }
    return Array.from(map.entries());
  }, [filtered]);

  const toggleCard = (id: number) => {
    setSelected(prev => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  return (
    <div className={styles.overlay} onClick={onClose}>
      <div className={styles.modal} onClick={e => e.stopPropagation()}>
        <div className={styles.modalHeader}>
          <h4>Liên kết thẻ</h4>
          <button className={styles.closeBtn} onClick={onClose}>✕</button>
        </div>

        <input
          className={styles.searchInput}
          placeholder="Tìm thẻ..."
          value={search}
          onChange={e => setSearch(e.target.value)}
          autoFocus
        />

        {cards.length === 0 && (
          <p className={styles.empty}>Không có thẻ nào trong workspace</p>
        )}

        {cards.length > 0 && filtered.length === 0 && (
          <p className={styles.empty}>Không tìm thấy thẻ phù hợp</p>
        )}

        <div className={styles.modalList}>
          {grouped.map(([boardName, boardCards]) => (
            <div key={boardName} className={styles.boardGroup}>
              <span className={styles.boardGroupTitle}>{boardName}</span>
              {boardCards.map(c => (
                <label key={c.id} className={styles.cardOption}>
                  <input
                    type="checkbox"
                    checked={selected.has(c.id)}
                    onChange={() => toggleCard(c.id)}
                  />
                  <div className={styles.cardOptionInfo}>
                    <span className={styles.cardOptionTitle}>{c.title}</span>
                    <span className={styles.cardOptionMeta}>#{c.id} · {c.listName}</span>
                  </div>
                </label>
              ))}
            </div>
          ))}
        </div>

        <div className={styles.modalFooter}>
          <button className={styles.cancelBtn} onClick={onClose}>Hủy</button>
          <button className={styles.saveBtn} onClick={() => onConfirm(Array.from(selected))}>
            Lưu ({selected.size})
          </button>
        </div>
      </div>
    </div>
  );
};

export default KeyResultRow;
