import { apiSlice } from './apiSlice';
import type {
  ConversationResponse,
  MessageResponse,
  ChatResponse,
  ChatRequest,
  SubtaskSuggestion,
  PrioritySuggestion,
  StandupSummary,
  ApiResponse as ApiRes,
} from '../types/ai.types';

export const aiApi = apiSlice.injectEndpoints({
  endpoints: (builder) => ({
    createConversation: builder.mutation<
      ApiRes<ConversationResponse>,
      { boardId?: number; workspaceId?: number }
    >({
      query: (params) => ({
        url: '/ai/conversations',
        method: 'POST',
        params,
      }),
      invalidatesTags: ['AiConversation'],
    }),

    getConversations: builder.query<ApiRes<ConversationResponse[]>, void>({
      query: () => '/ai/conversations',
      providesTags: ['AiConversation'],
    }),

    getMessages: builder.query<ApiRes<MessageResponse[]>, number>({
      query: (conversationId) => `/ai/conversations/${conversationId}/messages`,
      providesTags: (_result, _error, id) => [{ type: 'AiConversation', id }],
    }),

    sendMessage: builder.mutation<ApiRes<ChatResponse>, ChatRequest>({
      query: (body) => ({
        url: '/ai/chat',
        method: 'POST',
        body,
      }),
    }),

    deleteConversation: builder.mutation<ApiRes<void>, number>({
      query: (id) => ({
        url: `/ai/conversations/${id}`,
        method: 'DELETE',
      }),
      invalidatesTags: ['AiConversation'],
    }),

    breakdownTask: builder.mutation<ApiRes<SubtaskSuggestion[]>, { cardId: number }>({
      query: ({ cardId }) => ({
        url: '/ai/breakdown',
        method: 'POST',
        body: { cardId },
      }),
    }),

    suggestPriority: builder.mutation<ApiRes<PrioritySuggestion>, { cardId: number }>({
      query: ({ cardId }) => ({
        url: '/ai/suggest-priority',
        method: 'POST',
        body: { cardId },
      }),
    }),

    getStandup: builder.query<ApiRes<StandupSummary>, { workspaceId: number; userId?: number; date?: string }>({
      query: ({ workspaceId, userId, date }) => {
        const params = new URLSearchParams({ workspaceId: String(workspaceId) });
        if (userId) params.set('userId', String(userId));
        if (date) params.set('date', date);
        return `/ai/standup?${params}`;
      },
    }),

    getWorkspaceStandup: builder.query<ApiRes<StandupSummary[]>, { workspaceId: number; date?: string }>({
      query: ({ workspaceId, date }) => {
        const params = date ? `?date=${date}` : '';
        return `/ai/standup/workspace/${workspaceId}${params}`;
      },
    }),
  }),
});

export const {
  useCreateConversationMutation,
  useGetConversationsQuery,
  useGetMessagesQuery,
  useSendMessageMutation,
  useDeleteConversationMutation,
  useBreakdownTaskMutation,
  useSuggestPriorityMutation,
  useGetStandupQuery,
  useGetWorkspaceStandupQuery,
} = aiApi;
