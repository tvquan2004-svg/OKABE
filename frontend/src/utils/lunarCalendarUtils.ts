import { formatDateKey } from './calendarUtils';

interface LunarDate {
  lunarDay: number;
  lunarMonth: number;
  lunarYear: number;
  isHoliday: boolean;
  holidayName: string | null;
}

type LunarCache = Record<string, LunarDate>;

let cache: LunarCache = {};
const pendingRequests: Record<string, Promise<LunarDate | null>> = {};

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1';

async function fetchLunarDate(dateKey: string): Promise<LunarDate | null> {
  if (pendingRequests[dateKey]) return pendingRequests[dateKey];

  const promise = (async () => {
    try {
      const res = await fetch(`${API_BASE}/lunar?date=${dateKey}`, {
        credentials: 'include',
      });
      if (!res.ok) return null;
      const json = await res.json();
      return json.data as LunarDate;
    } catch {
      return null;
    } finally {
      delete pendingRequests[dateKey];
    }
  })();

  pendingRequests[dateKey] = promise;
  return promise;
}

export function getCachedLunarDate(date: Date): LunarDate | null {
  const key = formatDateKey(date);
  return cache[key] ?? null;
}

export async function loadLunarDate(date: Date): Promise<LunarDate | null> {
  const key = formatDateKey(date);
  if (cache[key]) return cache[key];

  const result = await fetchLunarDate(key);
  if (result) {
    cache[key] = result;
  }
  return result;
}

export async function preloadLunarMonth(year: number, month: number): Promise<void> {
  try {
    const res = await fetch(
      `${API_BASE}/lunar/month?month=${month}&year=${year}`,
      { credentials: 'include' }
    );
    if (!res.ok) return;
    const json = await res.json();
    const data: { days: LunarDate[] } = json.data;
    if (data?.days) {
      data.days.forEach((d, index) => {
        const dayNum = index + 1;
        const dateKey = `${year}-${String(month).padStart(2, '0')}-${String(dayNum).padStart(2, '0')}`;
        cache[dateKey] = d;
      });
    }
  } catch {
    // Silently fail
  }
}

export function clearLunarCache(): void {
  cache = {};
}

export function formatLunarDate(date: Date): string | null {
  const ld = getCachedLunarDate(date);
  if (!ld) return null;
  return `${ld.lunarDay}/${ld.lunarMonth} âm lịch`;
}

export function getHolidayInfo(date: Date): { isHoliday: boolean; name: string | null } {
  const ld = getCachedLunarDate(date);
  if (!ld) return { isHoliday: false, name: null };
  return { isHoliday: ld.isHoliday, name: ld.holidayName };
}
