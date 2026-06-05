import { apiSlice } from './apiSlice';

export interface LunarDateResponse {
  lunarDay: number;
  lunarMonth: number;
  lunarYear: number;
  isHoliday: boolean;
  holidayName: string | null;
}

export interface LunarMonthResponse {
  month: number;
  year: number;
  days: LunarDateResponse[];
}

export const lunarApi = apiSlice.injectEndpoints({
  endpoints: (builder) => ({
    getLunarDate: builder.query<LunarDateResponse, string>({
      query: (date) => `/lunar?date=${date}`,
    }),
    getLunarMonth: builder.query<LunarMonthResponse, { month: number; year: number }>({
      query: ({ month, year }) => `/lunar/month?month=${month}&year=${year}`,
    }),
  }),
});

export const { useGetLunarDateQuery, useGetLunarMonthQuery } = lunarApi;
