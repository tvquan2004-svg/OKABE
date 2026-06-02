import { useState, type ReactNode } from 'react';
import { useFocusTimer } from '../../contexts/FocusTimerContext';
import styles from './PomodoroWidget.module.css';

interface PomodoroWidgetProps {
  cardId: number;
  cardTitle?: string;
  totalFocusMinutes?: number;
}

const DURATION_OPTIONS = [5, 10, 15, 20, 25, 30, 45, 60];

function DnDGuard({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <span
      className={className}
      onPointerDown={(e) => e.stopPropagation()}
      onMouseDown={(e) => e.stopPropagation()}
      onTouchStart={(e) => e.stopPropagation()}
    >
      {children}
    </span>
  );
}

function PomodoroWidget({ cardId, cardTitle = '', totalFocusMinutes = 0 }: PomodoroWidgetProps) {
  const { state, start } = useFocusTimer();
  const [showPicker, setShowPicker] = useState(false);
  const [selectedMin, setSelectedMin] = useState(25);

  const isActiveOnThisCard = state.cardId === cardId && state.isRunning;

  const handleClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (isActiveOnThisCard) return;
    setShowPicker(!showPicker);
  };

  const handleStart = async (e: React.MouseEvent, minutes: number) => {
    e.stopPropagation();
    setSelectedMin(minutes);
    setShowPicker(false);
    try {
      await start(cardId, cardTitle, minutes, totalFocusMinutes);
    } catch (err) {
      console.error('[Pomodoro] start failed:', err);
    }
  };

  return (
    <DnDGuard className={styles.wrapper}>
      <button
        className={`${styles.iconBtn} ${isActiveOnThisCard ? styles.active : ''}`}
        onClick={handleClick}
        title={isActiveOnThisCard ? 'Đang focus...' : 'Pomodoro'}
      >
        🍅
        {isActiveOnThisCard && (
          <span className={styles.miniTimer}>
            {String(Math.floor(state.remaining / 60)).padStart(2, '0')}:
            {String(state.remaining % 60).padStart(2, '0')}
          </span>
        )}
      </button>

      {showPicker && !state.isRunning && (
        <div className={styles.picker}>
          <div className={styles.pickerTitle}>Chọn thời gian</div>
          <div className={styles.pickerGrid}>
            {DURATION_OPTIONS.map((m) => (
              <button
                key={m}
                className={`${styles.pickerBtn} ${m === selectedMin ? styles.pickerBtnActive : ''}`}
                onClick={(e) => handleStart(e, m)}
              >
                {m}ph
              </button>
            ))}
          </div>
        </div>
      )}
    </DnDGuard>
  );
}

export default PomodoroWidget;
