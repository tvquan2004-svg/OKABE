import { describe, it, expect } from 'vitest';
import { groupCardsByDate, getCalendarDays, formatDateKey } from './calendarUtils';
import { CardItem } from '../services/boardApi';

describe('calendarUtils', () => {
  describe('groupCardsByDate', () => {
    it('should group cards by due date string', () => {
      const mockCards = [
        { id: 1, dueDate: '2026-04-23T10:00:00Z', title: 'Task 1' },
        { id: 2, dueDate: '2026-04-23T15:00:00Z', title: 'Task 2' },
        { id: 3, dueDate: '2026-04-24T12:00:00Z', title: 'Task 3' },
        { id: 4, dueDate: null, title: 'Task 4' },
      ] as CardItem[];

      const grouped = groupCardsByDate(mockCards);
      
      expect(Object.keys(grouped)).toHaveLength(2);
      expect(grouped['2026-04-23']).toBeDefined();
      expect(grouped['2026-04-23']!).toHaveLength(2);
      expect(grouped['2026-04-23']![0]!.title).toBe('Task 1');
      expect(grouped['2026-04-24']).toBeDefined();
      expect(grouped['2026-04-24']!).toHaveLength(1);
    });
  });

  describe('getCalendarDays', () => {
    it('should return 42 days for the calendar grid', () => {
      const days = getCalendarDays(2026, 3); // April 2026
      expect(days).toHaveLength(42);
    });

    it('should include days from previous month if current month starts late in week', () => {
      // April 1st 2026 is a Wednesday (3)
      const days = getCalendarDays(2026, 3);
      expect(days[0]!.getMonth()).toBe(2); // March
      expect(days[0]!.getDate()).toBe(29); // March 29th (Sun)
      expect(days[3]!.getMonth()).toBe(3); // April
      expect(days[3]!.getDate()).toBe(1); // April 1st
    });
  });

  describe('formatDateKey', () => {
    it('should format date correctly as YYYY-MM-DD', () => {
      const date = new Date(2026, 3, 23); // April 23
      expect(formatDateKey(date)).toBe('2026-04-23');
    });
  });
});
