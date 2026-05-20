import { apiSlice } from './apiSlice';

export interface Objective {
  id: number;
  title: string;
  description: string | null;
  quarter: string;
  progress: number;
  createdBy: number;
  createdAt: string;
  updatedAt: string;
  keyResults: KeyResult[];
}

export interface KeyResult {
  id: number;
  title: string;
  targetValue: number | null;
  currentValue: number;
  unit: string;
  linkedCards: number[];
  createdAt: string;
}

interface ApiRes<T> { success: boolean; data: T; message: string; }

export const okrApi = apiSlice.injectEndpoints({
  endpoints: (builder) => ({
    getObjectives: builder.query<ApiRes<Objective[]>, { workspaceId: number; quarter?: string }>({
      query: ({ workspaceId, quarter }) =>
        `/workspaces/${workspaceId}/objectives${quarter ? `?quarter=${quarter}` : ''}`,
      providesTags: (_result, _error, { workspaceId }) => [{ type: 'OKR', id: workspaceId }],
    }),
    getObjective: builder.query<ApiRes<Objective>, { workspaceId: number; id: number }>({
      query: ({ workspaceId, id }) => `/workspaces/${workspaceId}/objectives/${id}`,
      providesTags: (_result, _error, { id }) => [{ type: 'OKR', id }],
    }),
    createObjective: builder.mutation<ApiRes<Objective>, { workspaceId: number; title: string; description?: string; quarter: string }>({
      query: ({ workspaceId, ...body }) => ({
        url: `/workspaces/${workspaceId}/objectives`,
        method: 'POST',
        body,
      }),
      invalidatesTags: (_result, _error, { workspaceId }) => [{ type: 'OKR', id: workspaceId }],
    }),
    addKeyResult: builder.mutation<ApiRes<KeyResult>, { objectiveId: number; title: string; targetValue?: number; unit?: string }>({
      query: ({ objectiveId, ...body }) => ({
        url: `/objectives/${objectiveId}/key-results`,
        method: 'POST',
        body,
      }),
      invalidatesTags: ['OKR'],
    }),
    linkCardsToKeyResult: builder.mutation<ApiRes<void>, { keyResultId: number; cardIds: number[] }>({
      query: ({ keyResultId, cardIds }) => ({
        url: `/key-results/${keyResultId}/cards`,
        method: 'POST',
        body: cardIds,
      }),
      invalidatesTags: ['OKR'],
    }),
    deleteObjective: builder.mutation<ApiRes<void>, number>({
      query: (id) => ({
        url: `/objectives/${id}`,
        method: 'DELETE',
      }),
      invalidatesTags: ['OKR'],
    }),
    recalculateProgress: builder.mutation<ApiRes<Objective>, number>({
      query: (id) => ({
        url: `/objectives/${id}/recalculate`,
        method: 'POST',
      }),
      invalidatesTags: ['OKR'],
    }),
    getOkrTree: builder.query<ApiRes<Objective[]>, { workspaceId: number; quarter?: string }>({
      query: ({ workspaceId, quarter }) =>
        `/workspaces/${workspaceId}/okr-tree${quarter ? `?quarter=${quarter}` : ''}`,
      providesTags: (_result, _error, { workspaceId }) => [{ type: 'OKR', id: workspaceId }],
    }),
  }),
});

export const {
  useGetObjectivesQuery,
  useGetObjectiveQuery,
  useCreateObjectiveMutation,
  useAddKeyResultMutation,
  useLinkCardsToKeyResultMutation,
  useDeleteObjectiveMutation,
  useRecalculateProgressMutation,
  useGetOkrTreeQuery,
} = okrApi;
