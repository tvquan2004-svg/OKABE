import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';

export const apiSlice = createApi({
  reducerPath: 'api',
  baseQuery: fetchBaseQuery({
    baseUrl: '/api/v1',
    prepareHeaders: (headers) => {
      const token = document.cookie
        .split('; ')
        .find(row => row.startsWith('access_token='))
        ?.split('=')[1];
      if (token) {
        headers.set('Authorization', `Bearer ${token}`);
      }
      return headers;
    },
  }),
  tagTypes: ['Board', 'List', 'Card', 'Workspace', 'User'],
  endpoints: () => ({}),
});
