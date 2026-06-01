import {
  createContext,
  useContext,
  useState,
  useEffect,
  useRef,
  useCallback,
  type ReactNode,
} from 'react';
import {
  useStartFocusMutation,
  useStopFocusMutation,
} from '../services/focusApi';

interface FocusTimerState {
  cardId: number | null;
  cardTitle: string;
  remaining: number;
  duration: number;
  totalFocusMinutes: number;
  isRunning: boolean;
  isPaused: boolean;
}

interface FocusTimerContextValue {
  state: FocusTimerState;
  start: (cardId: number, cardTitle: string, durationMinutes?: number, totalFocusMinutes?: number) => Promise<void>;
  stop: () => Promise<void>;
  pause: () => void;
  resume: () => void;
  setDuration: (minutes: number) => void;
}

const defaultDuration = 25 * 60;

const defaultState: FocusTimerState = {
  cardId: null,
  cardTitle: '',
  remaining: defaultDuration,
  duration: defaultDuration,
  totalFocusMinutes: 0,
  isRunning: false,
  isPaused: false,
};

const FocusTimerContext = createContext<FocusTimerContextValue | null>(null);

export function FocusTimerProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<FocusTimerState>(defaultState);
  const [startFocus] = useStartFocusMutation();
  const [stopFocus] = useStopFocusMutation();
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const audioRef = useRef<HTMLAudioElement | null>(null);

  const playAlarm = useCallback(() => {
    try {
      if (!audioRef.current) {
        audioRef.current = new Audio(
          'data:audio/wav;base64,UklGRnoGAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YQoGAACAf39/f4B/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+AgH9/f3+'
        );
      }
      audioRef.current.currentTime = 0;
      audioRef.current.play().catch(() => {});
    } catch {
      // Ignore audio errors
    }
  }, []);

  useEffect(() => {
    if (state.isRunning && !state.isPaused && state.cardId != null) {
      intervalRef.current = setInterval(() => {
        setState((prev) => {
          if (prev.remaining <= 1) {
            clearInterval(intervalRef.current!);
            intervalRef.current = null;
            playAlarm();
            return { ...prev, remaining: prev.duration, isRunning: false, isPaused: false };
          }
          return { ...prev, remaining: prev.remaining - 1 };
        });
      }, 1000);
    }
    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current);
    };
  }, [state.isRunning, state.isPaused, state.cardId, playAlarm]);

  const start = async (cardId: number, cardTitle: string, durationMinutes = 25, totalFocusMinutes = 0) => {
    const duration = durationMinutes * 60;
    // Optimistic: show timer immediately, call API in background
    setState({
      cardId,
      cardTitle,
      remaining: duration,
      duration,
      totalFocusMinutes,
      isRunning: true,
      isPaused: false,
    });
    try {
      await startFocus({ cardId, durationMinutes }).unwrap();
    } catch (err) {
      console.error('[FocusTimer] API start failed, rolling back', err);
      setState(defaultState);
    }
  };

  const stop = async () => {
    if (state.cardId != null) {
      try {
        const res = await stopFocus(state.cardId).unwrap();
        setState((prev) => ({
          ...prev,
          isRunning: false,
          isPaused: false,
          remaining: prev.duration,
          totalFocusMinutes: res.totalFocusMinutes,
        }));
      } catch {
        setState((prev) => ({
          ...prev,
          isRunning: false,
          isPaused: false,
          remaining: prev.duration,
        }));
      }
    }
  };

  const pause = () => {
    setState((prev) => ({ ...prev, isPaused: true }));
  };

  const resume = () => {
    setState((prev) => ({ ...prev, isPaused: false }));
  };

  const setDuration = (minutes: number) => {
    const duration = Math.max(1, Math.min(120, minutes)) * 60;
    setState((prev) => ({
      ...prev,
      duration,
      remaining: prev.isRunning || prev.isPaused ? prev.remaining : duration,
    }));
  };

  return (
    <FocusTimerContext.Provider value={{ state, start, stop, pause, resume, setDuration }}>
      {children}
    </FocusTimerContext.Provider>
  );
}

export function useFocusTimer() {
  const ctx = useContext(FocusTimerContext);
  if (!ctx) throw new Error('useFocusTimer must be used within FocusTimerProvider');
  return ctx;
}
