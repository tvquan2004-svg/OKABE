import { CardItem } from '../services/boardApi';

/**
 * Groups cards by their due date (YYYY-MM-DD).
 */
export const groupCardsByDate = (cards: CardItem[]): Record<string, CardItem[]> => {
  return cards.reduce((groups: Record<string, CardItem[]>, card) => {
    if (!card.dueDate) return groups;
    
    const date = card.dueDate.split('T')[0];
    if (!date) return groups;

    if (!groups[date]) {
      groups[date] = [];
    }
    groups[date].push(card);
    return groups;
  }, {});
};

/**
 * Returns an array of dates representing the calendar grid for a given month/year.
 * Includes padding days from previous and next months to fill the 7-column grid.
 */
export const getCalendarDays = (year: number, month: number): Date[] => {
  const firstDayOfMonth = new Date(year, month, 1);
  const lastDayOfMonth = new Date(year, month + 1, 0);
  
  const days: Date[] = [];
  
  // Padding from previous month
  const firstDayOfWeek = firstDayOfMonth.getDay(); // 0 (Sun) to 6 (Sat)
  const prevMonthLastDay = new Date(year, month, 0).getDate();
  
  for (let i = firstDayOfWeek - 1; i >= 0; i--) {
    days.push(new Date(year, month - 1, prevMonthLastDay - i));
  }
  
  // Days of current month
  for (let i = 1; i <= lastDayOfMonth.getDate(); i++) {
    days.push(new Date(year, month, i));
  }
  
  // Padding from next month
  const remainingDays = 42 - days.length; // 6 rows * 7 days = 42
  for (let i = 1; i <= remainingDays; i++) {
    days.push(new Date(year, month + 1, i));
  }
  
  return days;
};

/**
 * Formats a Date object to YYYY-MM-DD string in local time.
 */
export const formatDateKey = (date: Date): string => {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
};

/**
 * Returns CSS variable or color based on card priority.
 */
export const getPriorityColor = (priority: string): string => {
  switch (priority.toUpperCase()) {
    case 'CRITICAL':
      return 'var(--color-error)';
    case 'HIGH':
      return 'var(--color-warning)';
    case 'MEDIUM':
      return 'var(--color-info)';
    case 'LOW':
      return 'var(--color-text-muted)';
    default:
      return 'var(--color-text-muted)';
  }
};
