import { apiSlice } from './apiSlice';
import type {
  ConversationResponse,
  MessageResponse,
  ChatResponse,
  ChatRequest,
  SubtaskSuggestion,
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
  }),
});

export const {
  useCreateConversationMutation,
  useGetConversationsQuery,
  useGetMessagesQuery,
  useSendMessageMutation,
  useDeleteConversationMutation,
  useBreakdownTaskMutation,
} = aiApi;
