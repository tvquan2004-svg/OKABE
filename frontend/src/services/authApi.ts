import { apiSlice } from './apiSlice';

export interface UserInfo {
  id: number;
  email: string;
  username: string;
  avatarUrl: string | null;
  is2faEnabled: boolean;
}

export interface AuthResponse {
  accessToken?: string;
  refreshToken?: string;
  tokenType?: string;
  user?: UserInfo;
  needsRegistration?: boolean;
  email?: string;
  avatarUrl?: string;
  googleName?: string;
  requires2fa?: boolean;
  tempToken?: string;
}

export interface ApiResponseWrapper<T> {
  success: boolean;
  data: T;
  message: string;
}

interface LoginRequest {
  email: string;
  password: string;
}

interface RegisterRequest {
  username: string;
  email: string;
  password: string;
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
    googleLogin: builder.mutation<ApiResponseWrapper<AuthResponse>, { idToken?: string; accessToken?: string; username?: string }>({
      query: (body) => ({
        url: '/auth/google',
        method: 'POST',
        body,
      }),
    }),
    verifyEmail: builder.query<ApiResponseWrapper<void>, string>({
      query: (token) => `/auth/verify-email?token=${token}`,
    }),
    setup2fa: builder.mutation<ApiResponseWrapper<{ secret: string; qrCodeUri: string }>, void>({
      query: () => ({
        url: '/auth/2fa/setup',
        method: 'POST',
      }),
    }),
    verifySetup2fa: builder.mutation<ApiResponseWrapper<string[]>, { secret: string; code: number }>({
      query: (body) => ({
        url: '/auth/2fa/verify-setup',
        method: 'POST',
        body,
      }),
      invalidatesTags: ['User'],
    }),
    disable2fa: builder.mutation<ApiResponseWrapper<void>, { code: number }>({
      query: (body) => ({
        url: '/auth/2fa/disable',
        method: 'POST',
        body,
      }),
      invalidatesTags: ['User'],
    }),
    validate2fa: builder.mutation<ApiResponseWrapper<AuthResponse>, { tempToken: string; code: string; isBackupCode: boolean }>({
      query: (body) => ({
        url: '/auth/2fa/validate',
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
  useGoogleLoginMutation,
  useVerifyEmailQuery,
  useSetup2faMutation,
  useVerifySetup2faMutation,
  useDisable2faMutation,
  useValidate2faMutation,
} = authApi;
