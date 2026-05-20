import { apiSlice } from './apiSlice';

export interface Suggestion {
  id: number;
  type: string;
  message: string;
  cardId: number | null;
  actionUrl: string;
}

interface ApiRes<T> { success: boolean; data: T; message: string; }

export const suggestionApi = apiSlice.injectEndpoints({
  endpoints: (builder) => ({
    getSuggestions: builder.query<ApiRes<Suggestion[]>, number>({
      query: (workspaceId) => `/workspaces/${workspaceId}/suggestions`,
      providesTags: ['Suggestion'],
    }),
    dismissSuggestion: builder.mutation<ApiRes<void>, { type: string; cardId: number | null; workspaceId: number }>({
      query: (body) => ({
        url: `/suggestions/dismiss?type=${body.type}&cardId=${body.cardId ?? ''}&workspaceId=${body.workspaceId}`,
        method: 'POST',
      }),
      invalidatesTags: ['Suggestion'],
    }),
  }),
});

export const {
  useGetSuggestionsQuery,
  useDismissSuggestionMutation,
} = suggestionApi;
