export interface FocusSessionResponse {
  id: number;
  cardId: number;
  userId: number;
  userName: string;
  startedAt: string;
  endedAt: string | null;
  durationMinutes: number;
  completed: boolean;
  totalFocusMinutes: number;
}

export interface DailyFocus {
  date: string;
  minutes: number;
}

export interface TopCard {
  cardId: number;
  cardTitle: string;
  sessions: number;
  totalMinutes: number;
}

export interface FocusStatsResponse {
  todayMinutes: number;
  weekMinutes: number;
  monthMinutes: number;
  weekChangePercent: number;
  dailyBreakdown: DailyFocus[];
  topCards: TopCard[];
}
