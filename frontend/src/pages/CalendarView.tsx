import React, { useState, useMemo, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  FiChevronLeft,
  FiChevronRight,
  FiFilter,
  FiArrowLeft,
  FiMoon
} from 'react-icons/fi';
import {
  useGetBoardQuery,
  useUpdateCardMutation,
  CardItem,
  CardSearchParams
} from '../services/boardApi';
import { useGetWorkspaceMembersQuery } from '../services/workspaceApi';
import {
  getCalendarDays,
  groupCardsByDate,
  formatDateKey,
  getPriorityColor
} from '../utils/calendarUtils';
import {
  formatLunarDate,
  getHolidayInfo,
  preloadLunarMonth,
} from '../utils/lunarCalendarUtils';
import CardDetailModal from '../components/board/CardDetailModal';
import { BoardFilter } from '../components/board/BoardFilter';
import styles from './CalendarView.module.css';
import {
  DndContext,
  DragEndEvent,
  PointerSensor,
  useSensor,
  useSensors,
  closestCorners,
  useDroppable,
  useDraggable,
} from '@dnd-kit/core';

const WEEK_DAYS = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];

const CalendarView: React.FC = () => {
  const { boardId } = useParams<{ boardId: string }>();
  const navigate = useNavigate();
  const id = Number(boardId);

  const [currentDate, setCurrentDate] = useState(new Date());
  const [selectedCard, setSelectedCard] = useState<CardItem | null>(null);
  const [showFilters, setShowFilters] = useState(false);
  const [showLunar, setShowLunar] = useState(false);
  const [filters, setFilters] = useState<CardSearchParams>({});

  const { data: boardData, isLoading } = useGetBoardQuery(id);
  const { data: membersData } = useGetWorkspaceMembersQuery(boardData?.data.workspaceId ?? 0, {
    skip: !boardData?.data.workspaceId,
  });

  const [updateCard] = useUpdateCardMutation();

  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: {
        distance: 5,
      },
    })
  );

  useEffect(() => {
    if (showLunar) {
      preloadLunarMonth(currentDate.getFullYear(), currentDate.getMonth() + 1);
    }
  }, [showLunar, currentDate]);

  const board = boardData?.data;
  const allCards = useMemo(() => {
    return board?.lists?.flatMap(l => l.cards) ?? [];
  }, [board]);

  const allLabels = useMemo(() => {
    return board?.lists?.flatMap(l => l.cards).flatMap(c => c.labels) ?? [];
  }, [board]);

  const filteredCards = useMemo(() => {
    return allCards.filter(card => {
      if (!card.dueDate) return false;
      if (card.isArchived) return false;

      if (filters.priorities && filters.priorities.length > 0) {
        if (!filters.priorities.includes(card.priority)) return false;
      }

      if (filters.labelIds && filters.labelIds.length > 0) {
        const cardLabelIds = card.labels.map(l => l.id);
        if (!filters.labelIds.some(id => cardLabelIds.includes(id))) return false;
      }

      if (filters.assigneeIds && filters.assigneeIds.length > 0) {
        const cardMemberIds = card.members.map(m => m.id);
        if (!filters.assigneeIds.some(id => cardMemberIds.includes(id))) return false;
      }

      if (filters.keyword) {
        const keyword = filters.keyword.toLowerCase();
        return card.title.toLowerCase().includes(keyword) ||
               (card.description?.toLowerCase().includes(keyword) ?? false);
      }

      return true;
    });
  }, [allCards, filters]);

  const groupedCards = useMemo(() => groupCardsByDate(filteredCards), [filteredCards]);

  const calendarDays = useMemo(() => {
    return getCalendarDays(currentDate.getFullYear(), currentDate.getMonth());
  }, [currentDate]);

  const handlePrevMonth = () => {
    setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() - 1, 1));
  };

  const handleNextMonth = () => {
    setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 1));
  };

  const handleToday = () => {
    setCurrentDate(new Date());
  };

  const handleDragEnd = async (event: DragEndEvent) => {
    const { active, over } = event;
    if (!over) return;

    const cardId = Number(active.id);
    const dateKey = over.id as string;

    const card = allCards.find(c => c.id === cardId);
    if (card && formatDateKey(new Date(card.dueDate!)) !== dateKey) {
      try {
        const targetDate = new Date(dateKey);
        targetDate.setHours(12, 0, 0, 0);

        await updateCard({
          id: cardId,
          boardId: id,
          body: { dueDate: targetDate.toISOString() }
        }).unwrap();
      } catch (err) {
        console.error('Failed to update card due date:', err);
      }
    }
  };

  if (isLoading) return <div className={styles.loading}>Đang tải lịch...</div>;
  if (!board) return <div className={styles.loading}>Không tìm thấy bảng</div>;

  const currentMonthName = currentDate.toLocaleString('vi-VN', { month: 'long', year: 'numeric' });

  return (
    <div className={styles.calendarContainer}>
      <header className={styles.calendarHeader}>
        <div className={styles.headerLeft}>
          <button className="btn btn-outline" onClick={() => navigate(`/board/${id}`)}>
            <FiArrowLeft /> Quay lại bảng
          </button>
          <h1 className={styles.monthTitle}>
            {currentMonthName.charAt(0).toUpperCase() + currentMonthName.slice(1)}
          </h1>
          <div className={styles.headerActions}>
            <button className="btn btn-outline" onClick={handlePrevMonth}><FiChevronLeft /></button>
            <button className="btn btn-outline" onClick={handleToday}>Hôm nay</button>
            <button className="btn btn-outline" onClick={handleNextMonth}><FiChevronRight /></button>
          </div>
        </div>

        <div className={styles.headerRight}>
          <label className={styles.lunarToggle}>
            <FiMoon size={16} />
            <span>Hiển thị âm lịch</span>
            <input
              type="checkbox"
              checked={showLunar}
              onChange={(e) => setShowLunar(e.target.checked)}
            />
            <span className={styles.toggleSlider} />
          </label>
          <button
            className={`btn ${showFilters ? 'btn-primary' : 'btn-outline'}`}
            onClick={() => setShowFilters(!showFilters)}
          >
            <FiFilter /> Bộ lọc
          </button>
        </div>
      </header>

      {showFilters && (
        <div className={styles.filterBar}>
          <BoardFilter
            labels={allLabels.reduce((acc: typeof allLabels, curr) => acc.find(x => x.id === curr.id) ? acc : [...acc, curr], [])}
            members={membersData?.data.map(m => ({
              id: m.userId,
              username: m.username,
              email: m.email,
              avatarUrl: m.avatarUrl
            })) ?? []}
            onFilterChange={setFilters}
          />
        </div>
      )}

      <DndContext
        sensors={sensors}
        collisionDetection={closestCorners}
        onDragEnd={handleDragEnd}
      >
        <div className={styles.calendarGridWrapper}>
          <div className={styles.weekDaysHeader}>
            {WEEK_DAYS.map(day => (
              <div key={day} className={styles.weekDay}>{day}</div>
            ))}
          </div>

          <div className={styles.calendarGrid}>
            {calendarDays.map((day, idx) => {
              const dateKey = formatDateKey(day);
              const isToday = dateKey === formatDateKey(new Date());
              const isCurrentMonth = day.getMonth() === currentDate.getMonth();
              const dayCards = groupedCards[dateKey] ?? [];
              const lunarText = showLunar ? formatLunarDate(day) : null;
              const holiday = showLunar ? getHolidayInfo(day) : { isHoliday: false, name: null };

              return (
                <CalendarDayCell
                  key={idx}
                  day={day}
                  dateKey={dateKey}
                  isToday={isToday}
                  isCurrentMonth={isCurrentMonth}
                  cards={dayCards}
                  onCardClick={setSelectedCard}
                  lunarText={lunarText}
                  isHoliday={holiday.isHoliday}
                  holidayName={holiday.name}
                />
              );
            })}
          </div>
        </div>

        {/* Mobile View */}
        <div className={styles.mobileList}>
          {Object.entries(groupedCards)
            .sort(([a], [b]) => a.localeCompare(b))
            .map(([date, cards]) => (
              <div key={date} className={styles.mobileDayGroup}>
                <h3 className={styles.mobileDayTitle}>
                  {date}
          {showLunar && <LunarLabel dateStr={date} />}
                </h3>
                <div className={styles.cardList}>
                  {cards.map(card => (
                    <div
                      key={card.id}
                      className={styles.cardChip}
                      style={{ borderLeftColor: getPriorityColor(card.priority) }}
                      onClick={() => setSelectedCard(card)}
                    >
                      {card.title}
                    </div>
                  ))}
                </div>
              </div>
            ))}
        </div>
      </DndContext>

      {selectedCard && (
        <CardDetailModal
          card={allCards.find(c => c.id === selectedCard.id) || selectedCard}
          boardId={id}
          workspaceId={board.workspaceId}
          onClose={() => setSelectedCard(null)}
          priorityColor={(p) => getPriorityColor(p)}
        />
      )}
    </div>
  );
};

interface CalendarDayCellProps {
  day: Date;
  dateKey: string;
  isToday: boolean;
  isCurrentMonth: boolean;
  cards: CardItem[];
  onCardClick: (card: CardItem) => void;
  lunarText: string | null;
  isHoliday: boolean;
  holidayName: string | null;
}

const CalendarDayCell: React.FC<CalendarDayCellProps> = ({
  day,
  dateKey,
  isToday,
  isCurrentMonth,
  cards,
  onCardClick,
  lunarText,
  isHoliday,
  holidayName
}) => {
  const { setNodeRef } = useDroppable({
    id: dateKey,
  });

  const displayCards = cards.slice(0, 3);
  const extraCount = cards.length - 3;

  const cellClass = [
    styles.dayCell,
    isToday ? styles.isToday : '',
    !isCurrentMonth ? styles.notCurrentMonth : '',
    isHoliday ? styles.holidayCell : '',
  ].filter(Boolean).join(' ');

  return (
    <div
      ref={setNodeRef}
      className={cellClass}
      title={isHoliday && holidayName ? holidayName : undefined}
    >
      <div className={styles.dayCellHeader}>
        <span className={styles.dayNumber}>{day.getDate()}</span>
      </div>
      {lunarText && (
        <div className={`${styles.lunarText} ${isHoliday ? styles.holidayLunarText : ''}`}>
          {lunarText}
        </div>
      )}
      <div className={styles.cardList}>
        {displayCards.map(card => (
          <DraggableCardChip
            key={card.id}
            card={card}
            onClick={() => onCardClick(card)}
          />
        ))}
        {extraCount > 0 && (
          <div className={styles.moreIndicator}>+ {extraCount} thẻ khác</div>
        )}
      </div>
    </div>
  );
};

const DraggableCardChip: React.FC<{ card: CardItem; onClick: () => void }> = ({ card, onClick }) => {
  const { attributes, listeners, setNodeRef, transform, isDragging } = useDraggable({
    id: card.id,
  });

  const style = transform ? {
    transform: `translate3d(${transform.x}px, ${transform.y}px, 0)`,
    zIndex: 100,
  } : undefined;

  return (
    <div
      ref={setNodeRef}
      {...listeners}
      {...attributes}
      className={styles.cardChip}
      style={{
        ...style,
        borderLeftColor: getPriorityColor(card.priority),
        opacity: isDragging ? 0.5 : 1,
      }}
      onClick={(e) => {
        e.stopPropagation();
        onClick();
      }}
    >
      {card.title}
    </div>
  );
};

function LunarLabel({ dateStr }: { dateStr: string }) {
  const parts = dateStr.split('-');
  if (parts.length !== 3) return null;
  const y = Number(parts[0]);
  const m = Number(parts[1]);
  const d = Number(parts[2]);
  const dt = new Date(y, m - 1, d);
  const lt = formatLunarDate(dt);
  if (!lt) return null;
  return <span className={styles.mobileLunar}> - {lt}</span>;
}

export default CalendarView;
