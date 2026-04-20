import { apiSlice } from './apiSlice';
import { type PaginatedRes } from './boardApi';

export interface Notification {
  id: number;
  actorId: number | null;
  actorName: string;
  actorAvatarUrl: string | null;
  type: string;
  entityType: string;
  entityId: number;
  message: string;
  isRead: boolean;
  createdAt: string;
}

interface ApiRes<T> { success: boolean; data: T; message: string; }

export const notificationApi = apiSlice.injectEndpoints({
  endpoints: (builder) => ({
    getNotifications: builder.query<ApiRes<PaginatedRes<Notification>>, { page?: number; size?: number }>({
      query: ({ page = 0, size = 20 }) => `/notifications?page=${page}&size=${size}`,
      providesTags: ['Notification'],
    }),
    getUnreadCount: builder.query<ApiRes<number>, void>({
      query: () => '/notifications/unread-count',
      providesTags: ['Notification'],
    }),
    markAsRead: builder.mutation<ApiRes<void>, number>({
      query: (id) => ({ url: `/notifications/${id}/read`, method: 'PUT' }),
      invalidatesTags: ['Notification'],
    }),
    markAllAsRead: builder.mutation<ApiRes<void>, void>({
      query: () => ({ url: '/notifications/read-all', method: 'PUT' }),
      invalidatesTags: ['Notification'],
    }),
  }),
});

export const {
  useGetNotificationsQuery,
  useGetUnreadCountQuery,
  useMarkAsReadMutation,
  useMarkAllAsReadMutation,
} = notificationApi;
