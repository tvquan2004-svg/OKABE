import { apiSlice } from './apiSlice';

export interface NotificationPreferences {
  emailAssigned: boolean;
  emailMentioned: boolean;
  emailDueSoon: boolean;
  emailInvited: boolean;
}

export const userApi = apiSlice.injectEndpoints({
  endpoints: (builder) => ({
    getNotificationPreferences: builder.query<NotificationPreferences, void>({
      query: () => '/users/me/notification-preferences',
      providesTags: ['NotificationPreferences'],
      transformResponse: (response: { data: NotificationPreferences }) => response.data,
    }),
    updateNotificationPreferences: builder.mutation<NotificationPreferences, NotificationPreferences>({
      query: (preferences) => ({
        url: '/users/me/notification-preferences',
        method: 'PUT',
        body: preferences,
      }),
      invalidatesTags: ['NotificationPreferences'],
      transformResponse: (response: { data: NotificationPreferences }) => response.data,
    }),
  }),
});

export const {
  useGetNotificationPreferencesQuery,
  useUpdateNotificationPreferencesMutation,
} = userApi;
