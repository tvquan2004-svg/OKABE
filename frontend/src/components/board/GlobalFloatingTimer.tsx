import { useState, useEffect, useRef, useMemo } from 'react';
import { useFocusTimer } from '../../contexts/FocusTimerContext';
import styles from './GlobalFloatingTimer.module.css';

const CIRCUMFERENCE = 2 * Math.PI * 45;

function GlobalFloatingTimer() {
  const { state, stop, pause, resume } = useFocusTimer();
  const [position, setPosition] = useState({ x: window.innerWidth - 210, y: 80 });
  const [dragging, setDragging] = useState(false);
  const [dragOffset, setDragOffset] = useState({ x: 0, y: 0 });
  const initialized = useRef(false);

  useEffect(() => {
    if (!initialized.current) {
      setPosition({ x: window.innerWidth - 210, y: 80 });
      initialized.current = true;
    }
  }, []);

  const handleMouseDown = (e: React.MouseEvent) => {
    setDragging(true);
    setDragOffset({ x: e.clientX - position.x, y: e.clientY - position.y });
  };

  useEffect(() => {
    if (!dragging) return;
    const handleMove = (e: MouseEvent) => {
      setPosition({
        x: Math.max(0, Math.min(window.innerWidth - 190, e.clientX - dragOffset.x)),
        y: Math.max(0, Math.min(window.innerHeight - 130, e.clientY - dragOffset.y)),
      });
    };
    const handleUp = () => setDragging(false);
    window.addEventListener('mousemove', handleMove);
    window.addEventListener('mouseup', handleUp);
    return () => {
      window.removeEventListener('mousemove', handleMove);
      window.removeEventListener('mouseup', handleUp);
    };
  }, [dragging, dragOffset]);

  if (!state.isRunning && !state.isPaused) return null;

  const minutes = Math.floor(state.remaining / 60);
  const seconds = state.remaining % 60;
  const totalSec = state.duration;
  const progressFraction = totalSec > 0 ? (totalSec - state.remaining) / totalSec : 0;
  const offset = CIRCUMFERENCE * (1 - progressFraction);
  const isLow = state.remaining <= 300;
  const isPaused = state.isPaused;

  const ringColor = useMemo(() => {
    const ratio = state.remaining / totalSec;
    if (ratio > 0.5) return 'var(--color-primary)';
    if (ratio > 0.25) return '#f59e0b';
    return '#ef4444';
  }, [state.remaining, totalSec]);

  return (
    <div
      className={`${styles.floating} ${dragging ? styles.dragging : ''} ${isLow && !isPaused ? styles.low : ''} ${isPaused ? styles.paused : ''}`}
      style={{ left: position.x, top: position.y }}
      onMouseDown={handleMouseDown}
    >
      <div className={styles.cardInfo}>{state.cardTitle}</div>
      <div className={styles.timerDisplay}>
        <svg className={styles.progressRing} viewBox="0 0 100 100">
          <circle className={styles.progressBg} cx="50" cy="50" r="45" />
          <circle
            className={styles.progressFill}
            cx="50" cy="50" r="45"
            style={{
              strokeDashoffset: offset,
              stroke: ringColor,
            }}
          />
        </svg>
        <span className={`${styles.timerText} ${isLow && !isPaused ? styles.timerLow : ''}`}>
          {String(minutes).padStart(2, '0')}:{String(seconds).padStart(2, '0')}
        </span>
      </div>
      <div className={styles.controls}>
        {isPaused ? (
          <button className={styles.controlBtn} onClick={resume} title="Tiếp tục">▶</button>
        ) : (
          <button className={styles.controlBtn} onClick={pause} title="Tạm dừng">⏸</button>
        )}
        <button className={styles.stopBtn} onClick={stop} title="Dừng">⏹</button>
      </div>
    </div>
  );
}

export default GlobalFloatingTimer;
