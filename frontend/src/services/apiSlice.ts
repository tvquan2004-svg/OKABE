import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
<<<<<<< HEAD
import type { RootState } from '../store';
=======
>>>>>>> ff3a2ec6328dbe8306d27b4f0eb9a549f3c65124

export const apiSlice = createApi({
  reducerPath: 'api',
  baseQuery: fetchBaseQuery({
    baseUrl: '/api/v1',
<<<<<<< HEAD
    prepareHeaders: (headers, { getState }) => {
      const token = (getState() as RootState).auth.accessToken;
=======
    prepareHeaders: (headers) => {
      const token = document.cookie
        .split('; ')
        .find(row => row.startsWith('access_token='))
        ?.split('=')[1];
>>>>>>> ff3a2ec6328dbe8306d27b4f0eb9a549f3c65124
      if (token) {
        headers.set('Authorization', `Bearer ${token}`);
      }
      return headers;
    },
  }),
  tagTypes: ['Board', 'List', 'Card', 'Workspace', 'User'],
  endpoints: () => ({}),
});
