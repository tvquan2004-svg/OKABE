import React from 'react';
import { useDroppable } from '@dnd-kit/core';
import { FiArchive, FiArrowDown } from 'react-icons/fi';
import styles from './BoardArchiveZone.module.css';

interface BoardArchiveZoneProps {
  isDragging: boolean;
}

const BoardArchiveZone: React.FC<BoardArchiveZoneProps> = ({ isDragging }) => {
  const { setNodeRef, isOver } = useDroppable({
    id: 'archive-zone',
  });

  if (!isDragging) return null;

  return (
    <div 
      ref={setNodeRef}
      className={`${styles.archiveZone} ${isOver ? styles.isOver : ''}`}
    >
      <div className={styles.content}>
        {isOver ? (
          <>
            <FiArrowDown className={styles.bounceIcon} />
            <span className={styles.text}>Thả vào đây để lưu trữ</span>
          </>
        ) : (
          <>
            <FiArchive className={styles.icon} />
            <span className={styles.text}>Kéo vào đây để lưu trữ bảng</span>
          </>
        )}
      </div>
      <div className={styles.glowEffect} />
    </div>
  );
};

export default BoardArchiveZone;
