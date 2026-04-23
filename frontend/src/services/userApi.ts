import { apiSlice } from './apiSlice';

export interface NotificationPreferences {
  emailAssigned: boolean;
  emailMentioned: boolean;
  emailDueSoon: boolean;
  emailInvited: boolean;
}

export const userApi = apiSlice.injectEndpoints({
  endpoints: (builder) => ({
    getMe: builder.query<UserResponse, void>({
      query: () => '/users/me',
      providesTags: ['User'],
      transformResponse: (response: { data: UserResponse }) => response.data,
    }),
    updateProfile: builder.mutation<UserResponse, { username: string; avatarUrl?: string }>({
      query: (body) => ({
        url: '/users/me',
        method: 'PUT',
        body,
      }),
      invalidatesTags: ['User'],
      transformResponse: (response: { data: UserResponse }) => response.data,
    }),
    uploadAvatar: builder.mutation<UserResponse, FormData>({
      query: (formData) => ({
        url: '/users/avatar',
        method: 'POST',
        body: formData,
      }),
      invalidatesTags: ['User'],
      transformResponse: (response: { data: UserResponse }) => response.data,
    }),
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

export interface UserResponse {
  id: number;
  username: string;
  email: string;
  avatarUrl: string | null;
  is2faEnabled: boolean;
}

export const {
  useGetMeQuery,
  useUpdateProfileMutation,
  useUploadAvatarMutation,
  useGetNotificationPreferencesQuery,
  useUpdateNotificationPreferencesMutation,
} = userApi;
