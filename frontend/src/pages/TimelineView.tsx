import React, { useState, useMemo, useRef, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
  FiArrowLeft,
  FiChevronLeft,
  FiChevronRight
} from 'react-icons/fi';
import { 
  useGetBoardQuery, 
  CardItem 
} from '../services/boardApi';
import CardDetailModal from '../components/board/CardDetailModal';
import styles from './TimelineView.module.css';

const DAY_WIDTH = 60;
const DAYS_TO_SHOW = 60; // Show 60 days of timeline

const TimelineView: React.FC = () => {
  const { boardId } = useParams<{ boardId: string }>();
  const navigate = useNavigate();
  const id = Number(boardId);

  const [selectedCard, setSelectedCard] = useState<CardItem | null>(null);
  const [viewDate, setViewDate] = useState(() => {
    const d = new Date();
    d.setHours(0, 0, 0, 0);
    return d;
  });
  const chartRef = useRef<HTMLDivElement>(null);

  // Start the timeline at the beginning of the view month
  const timelineStartDate = useMemo(() => {
    const d = new Date(viewDate);
    d.setDate(1); // Start at 1st of month
    // Go back 7 days to show context
    d.setDate(d.getDate() - 7);
    return d;
  }, [viewDate]);

  const timelineDays = useMemo(() => {
    const days = [];
    for (let i = 0; i < DAYS_TO_SHOW; i++) {
      const d = new Date(timelineStartDate);
      d.setDate(d.getDate() + i);
      days.push(d);
    }
    return days;
  }, [timelineStartDate]);

  const navigateMonth = (offset: number) => {
    const newDate = new Date(viewDate);
    newDate.setMonth(newDate.getMonth() + offset);
    setViewDate(newDate);
  };

  const goToToday = () => {
    const d = new Date();
    d.setHours(0, 0, 0, 0);
    setViewDate(d);
  };

  const { data: boardData, isLoading } = useGetBoardQuery(id);
  const board = boardData?.data;

  const cards = useMemo(() => {
    return board?.lists?.flatMap(l => l.cards).filter(c => !c.isArchived && (c.startDate || c.dueDate)) ?? [];
  }, [board]);

  // Scroll to "viewDate" on change
  useEffect(() => {
    if (chartRef.current) {
      const diff = Math.floor((viewDate.getTime() - timelineStartDate.getTime()) / (1000 * 60 * 60 * 24));
      if (diff > 0) {
        chartRef.current.scrollLeft = (diff * DAY_WIDTH) - 200;
      }
    }
  }, [viewDate, timelineStartDate]);

  if (isLoading) return <div className={styles.loading}>Đang tải dòng thời gian...</div>;
  if (!board) return <div className={styles.loading}>Không tìm thấy bảng</div>;

  const getTaskBarStyle = (card: CardItem) => {
    if (!card.startDate && !card.dueDate) return { display: 'none' };

    const start = card.startDate ? new Date(card.startDate) : new Date(card.dueDate!);
    const end = card.dueDate ? new Date(card.dueDate) : new Date(card.startDate!);
    
    const actualEnd = end < start ? start : end;

    const leftDiff = (start.getTime() - timelineStartDate.getTime()) / (1000 * 60 * 60 * 24);
    const durationDays = Math.max(0.5, (actualEnd.getTime() - start.getTime()) / (1000 * 60 * 60 * 24) + 1);

    const left = leftDiff * DAY_WIDTH;
    const width = durationDays * DAY_WIDTH;

    // Filter out cards that are completely outside the visible 60-day range to avoid huge widths
    if (left + width < 0 || left > DAYS_TO_SHOW * DAY_WIDTH) return { display: 'none' };

    const priorityColor = (p: string) => {
      switch (p.toUpperCase()) {
        case 'CRITICAL': return 'var(--color-error)';
        case 'HIGH': return 'var(--color-warning)';
        case 'MEDIUM': return 'var(--color-primary)';
        case 'LOW': return 'var(--color-text-muted)';
        default: return 'var(--color-primary)';
      }
    };

    return {
      left: `${left}px`,
      width: `${width}px`,
      backgroundColor: priorityColor(card.priority),
    };
  };

  return (
    <div className={styles.timelineContainer}>
      <header className={styles.timelineHeader}>
        <div className={styles.titleSection}>
          <button className="btn btn-outline" onClick={() => navigate(`/board/${id}`)}>
            <FiArrowLeft /> Quay lại
          </button>
          <h1 className={styles.timelineTitle}>{board.name}</h1>
        </div>

        <div className={styles.navSection}>
          <button className="btn btn-outline" onClick={() => navigateMonth(-1)}>
            <FiChevronLeft /> Tháng trước
          </button>
          <button className="btn btn-outline" onClick={goToToday}>
            Hôm nay
          </button>
          <div className={styles.currentMonth}>
            {viewDate.toLocaleDateString('vi-VN', { month: 'long', year: 'numeric' })}
          </div>
          <button className="btn btn-outline" onClick={() => navigateMonth(1)}>
            Tháng sau <FiChevronRight />
          </button>
        </div>
      </header>

      <div className={styles.timelineContent}>
        {/* Left Sidebar */}
        <div className={styles.sidebar}>
          <div className={styles.sidebarHeader}>Thẻ công việc</div>
          <div className={styles.sidebarContent}>
            {cards.map(card => {
              const dateObj = card.startDate ? new Date(card.startDate) : (card.dueDate ? new Date(card.dueDate) : null);
              const dateStr = dateObj ? dateObj.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' }) : '';
              
              return (
                <div 
                  key={card.id} 
                  className={styles.sidebarRow}
                  onClick={() => setSelectedCard(card)}
                >
                  <div className={styles.cardTitleInfo}>
                    <span className={styles.cardTitleText}>{card.title}</span>
                    {dateStr && <span className={styles.dateHint}>{dateStr}</span>}
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Right Chart Area */}
        <div className={styles.chartArea} ref={chartRef}>
          <div className={styles.chartGrid} style={{ width: DAYS_TO_SHOW * DAY_WIDTH }}>
            {/* Header with Days */}
            <div className={styles.chartHeader}>
              {timelineDays.map((day, idx) => {
                const isToday = day.toDateString() === new Date().toDateString();
                return (
                  <div key={idx} className={`${styles.headerDay} ${isToday ? styles.isTodayHeader : ''}`}>
                    <span className={styles.dayName}>
                      {day.toLocaleDateString('vi-VN', { weekday: 'short' })}
                    </span>
                    <span className={styles.dayNum}>{day.getDate()}</span>
                  </div>
                );
              })}
            </div>

            {/* Rows with Bars */}
            <div className={styles.chartRows}>
              {cards.map(card => (
                <div key={card.id} className={styles.chartRow}>
                  {/* Grid background lines */}
                  {timelineDays.map((_, idx) => (
                    <div key={idx} className={styles.gridLine} style={{ left: idx * DAY_WIDTH }} />
                  ))}
                  
                  {/* Task Bar */}
                  <div 
                    className={styles.taskBar} 
                    style={getTaskBarStyle(card)}
                    onClick={() => setSelectedCard(card)}
                  >
                    <span className={styles.taskBarLabel}>{card.title}</span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      {selectedCard && (
        <CardDetailModal
          card={board.lists?.flatMap(l => l.cards).find(c => c.id === selectedCard.id) || selectedCard}
          boardId={id}
          workspaceId={board.workspaceId}
          onClose={() => setSelectedCard(null)}
          priorityColor={(p) => {
             switch (p.toUpperCase()) {
                case 'CRITICAL': return '#ef4444';
                case 'HIGH': return '#f59e0b';
                case 'MEDIUM': return '#3b82f6';
                case 'LOW': return '#22c55e';
                default: return '#64748b';
              }
          }}
        />
      )}
    </div>
  );
};

export default TimelineView;
