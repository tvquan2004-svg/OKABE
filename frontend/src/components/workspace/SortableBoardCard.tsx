import type { CSSProperties, MouseEvent } from 'react';
import { CSS } from '@dnd-kit/utilities';
import { useSortable } from '@dnd-kit/sortable';
import { FiEdit2, FiTrash2, FiLayers, FiCheckSquare } from 'react-icons/fi';
import type { Board } from '../../services/boardApi';
import styles from './SortableBoardCard.module.css';

interface SortableBoardCardProps {
  board: Board;
  canManage: boolean;
  canReorder: boolean;
  onOpen: (boardId: number) => void;
  onEdit: (board: Board, event: MouseEvent<HTMLButtonElement>) => void;
  onDelete: (boardId: number, event: MouseEvent<HTMLButtonElement>) => void;
}

function SortableBoardCard({
  board,
  canManage,
  canReorder,
  onOpen,
  onEdit,
  onDelete,
}: SortableBoardCardProps) {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({
    id: board.id,
    disabled: !canReorder,
  });

  const cardStyle = {
    transform: CSS.Transform.toString(transform),
    transition,
    '--board-accent': board.background ?? '#6366f1',
  } as CSSProperties;

  return (
    <div
      ref={setNodeRef}
      style={cardStyle}
      className={`${styles.card} ${isDragging ? styles.dragging : ''}`}
      onClick={() => onOpen(board.id)}
      {...attributes}
      {...listeners}
    >
      <div className={styles.glassEffect} />
      <div className={styles.colorBar} />
      <div className={styles.content}>
        <div className={styles.headerRow}>
          <h3 className={styles.title}>{board.name}</h3>
          {canManage ? (
            <div className={styles.actions}>
              <button
                type="button"
                className={styles.iconButton}
                onClick={(event) => onEdit(board, event)}
                title="Chỉnh sửa bảng"
              >
                <FiEdit2 size={14} />
              </button>
              <button
                type="button"
                className={`${styles.iconButton} ${styles.deleteBtn}`}
                onClick={(event) => onDelete(board.id, event)}
                title="Xóa bảng"
              >
                <FiTrash2 size={14} />
              </button>
            </div>
          ) : null}
        </div>

        {board.description ? (
          <p className={styles.description}>{board.description}</p>
        ) : (
          <p className={styles.descriptionPlaceholder}>Không có mô tả cho bảng này</p>
        )}

        <div className={styles.stats}>
          <div className={styles.statItem}>
            <FiLayers size={14} />
            <span>{board.listCount ?? 0} danh sách</span>
          </div>
          <div className={styles.statItem}>
            <FiCheckSquare size={14} />
            <span>{board.totalCards ?? 0} thẻ</span>
          </div>
        </div>
      </div>
    </div>
  );
}

export default SortableBoardCard;
