import { createApi, fetchBaseQuery, BaseQueryFn, FetchArgs, FetchBaseQueryError } from '@reduxjs/toolkit/query/react';
import type { RootState } from '../store';
import { logout, setCredentials } from '../features/auth/authSlice';

// Biến để kiểm soát việc làm mới token
let isRefreshing = false;
let refreshSubscribers: ((token: string | null) => void)[] = [];

const onTokenRefreshed = (token: string | null) => {
  refreshSubscribers.forEach((callback) => callback(token));
  refreshSubscribers = [];
};

const addRefreshSubscriber = (callback: (token: string | null) => void) => {
  refreshSubscribers.push(callback);
};

const getBaseUrl = () => {
  const envUrl = import.meta.env.VITE_API_BASE_URL as string;
  const baseUrl = !envUrl ? 'http://localhost:8080/api/v1' : (envUrl.endsWith('/api/v1') ? envUrl : `${envUrl}/api/v1`);
  
  if (import.meta.env.DEV) {
    console.log('API Base URL:', baseUrl);
  }
  return baseUrl;
};

const baseQuery = fetchBaseQuery({
  baseUrl: getBaseUrl(),
  prepareHeaders: (headers, { getState }) => {
    const token = (getState() as RootState).auth.accessToken;
    if (token) {
      headers.set('Authorization', `Bearer ${token}`);
    }
    return headers;
  },
});

const baseQueryWithReauth: BaseQueryFn<
  string | FetchArgs,
  unknown,
  FetchBaseQueryError
> = async (args, api, extraOptions) => {
  let result = await baseQuery(args, api, extraOptions);
  
  if (result.error && (result.error.status === 401 || result.error.status === 403)) {
    if (!isRefreshing) {
      isRefreshing = true;
      const refreshToken = (api.getState() as RootState).auth.refreshToken;
      
      if (refreshToken) {
        try {
          const refreshResult: any = await baseQuery(
            {
              url: '/auth/refresh',
              method: 'POST',
              body: { refreshToken },
            },
            api,
            extraOptions
          );

          if (refreshResult.data) {
            const newAccessToken = refreshResult.data.data.accessToken;
            api.dispatch(setCredentials(refreshResult.data.data));
            onTokenRefreshed(newAccessToken);
            isRefreshing = false;
            
            // Thử lại yêu cầu ban đầu
            result = await baseQuery(args, api, extraOptions);
          } else {
            isRefreshing = false;
            onTokenRefreshed(null);
            api.dispatch(logout());
          }
        } catch (_err) {
          isRefreshing = false;
          onTokenRefreshed(null);
          api.dispatch(logout());
        }
      } else {
        api.dispatch(logout());
      }
    } else {
      // Nếu đang có tiến trình refresh khác, tạo một Promise để chờ nó xong
      return new Promise((resolve) => {
        addRefreshSubscriber((token) => {
          if (token) {
            resolve(baseQuery(args, api, extraOptions));
          } else {
            resolve(result); // Trả về lỗi 401 ban đầu nếu refresh thất bại
          }
        });
      });
    }
  }
  
  return result;
};

export const apiSlice = createApi({
  reducerPath: 'api',
  baseQuery: baseQueryWithReauth,
  tagTypes: ['Board', 'List', 'Card', 'Workspace', 'User', 'WorkspaceMember', 'Activity', 'Notification', 'Comment', 'NotificationPreferences', 'Template'],
  endpoints: () => ({}),
});
