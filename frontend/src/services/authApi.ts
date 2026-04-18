import { apiSlice } from './apiSlice';

interface LoginRequest {
  email: string;
  password: string;
}

interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

interface UserInfo {
  id: number;
  email: string;
  username: string;
  avatarUrl: string | null;
}

interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  user: UserInfo;
}

interface ApiResponseWrapper<T> {
  success: boolean;
  data: T;
  message: string;
}

export const authApi = apiSlice.injectEndpoints({
  endpoints: (builder) => ({
    login: builder.mutation<ApiResponseWrapper<AuthResponse>, LoginRequest>({
      query: (credentials) => ({
        url: '/auth/login',
        method: 'POST',
        body: credentials,
      }),
    }),
    register: builder.mutation<ApiResponseWrapper<AuthResponse>, RegisterRequest>({
      query: (userData) => ({
        url: '/auth/register',
        method: 'POST',
        body: userData,
      }),
    }),
    getMe: builder.query<ApiResponseWrapper<UserInfo>, void>({
      query: () => '/auth/me',
      providesTags: ['User'],
    }),
    refreshToken: builder.mutation<ApiResponseWrapper<AuthResponse>, { refreshToken: string }>({
      query: (body) => ({
        url: '/auth/refresh',
        method: 'POST',
        body,
      }),
    }),
  }),
});

export const {
  useLoginMutation,
  useRegisterMutation,
  useGetMeQuery,
  useRefreshTokenMutation,
} = authApi;
