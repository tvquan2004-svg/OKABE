import { apiSlice } from './apiSlice';
import type {
  FocusSessionResponse,
  FocusStatsResponse,
} from '../types/focus.types';

export const focusApi = apiSlice.injectEndpoints({
  endpoints: (builder) => ({
    startFocus: builder.mutation<FocusSessionResponse, { cardId: number; durationMinutes?: number }>({
      query: ({ cardId, durationMinutes }) => ({
        url: `/cards/${cardId}/focus/start`,
        method: 'POST',
        params: durationMinutes ? { durationMinutes } : undefined,
      }),
      invalidatesTags: ['Focus'],
    }),

    stopFocus: builder.mutation<FocusSessionResponse, number>({
      query: (cardId) => ({
        url: `/cards/${cardId}/focus/stop`,
        method: 'POST',
      }),
      invalidatesTags: ['Focus'],
    }),

    getFocusStats: builder.query<
      FocusStatsResponse,
      { from?: string; to?: string }
    >({
      query: (params) => ({
        url: '/users/me/focus-stats',
        params,
      }),
      providesTags: ['Focus'],
    }),
  }),
});

export const {
  useStartFocusMutation,
  useStopFocusMutation,
  useGetFocusStatsQuery,
} = focusApi;
