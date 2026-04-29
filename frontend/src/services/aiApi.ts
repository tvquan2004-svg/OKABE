import { apiSlice } from './apiSlice';
import type {
  ConversationResponse,
  MessageResponse,
  ChatResponse,
  ChatRequest,
  ApiResponse,
} from '../types/ai.types';

export const aiApi = apiSlice.injectEndpoints({
  endpoints: (builder) => ({
    createConversation: builder.mutation<
      ApiResponse<ConversationResponse>,
      { boardId?: number; workspaceId?: number }
    >({
      query: (params) => ({
        url: '/ai/conversations',
        method: 'POST',
        params,
      }),
      invalidatesTags: ['AiConversation'],
    }),

    getConversations: builder.query<ApiResponse<ConversationResponse[]>, void>({
      query: () => '/ai/conversations',
      providesTags: ['AiConversation'],
    }),

    getMessages: builder.query<ApiResponse<MessageResponse[]>, number>({
      query: (conversationId) => `/ai/conversations/${conversationId}/messages`,
      providesTags: (_result, _error, id) => [{ type: 'AiConversation', id }],
    }),

    sendMessage: builder.mutation<ApiResponse<ChatResponse>, ChatRequest>({
      query: (body) => ({
        url: '/ai/chat',
        method: 'POST',
        body,
      }),
    }),

    deleteConversation: builder.mutation<ApiResponse<void>, number>({
      query: (id) => ({
        url: `/ai/conversations/${id}`,
        method: 'DELETE',
      }),
      invalidatesTags: ['AiConversation'],
    }),
  }),
});

export const {
  useCreateConversationMutation,
  useGetConversationsQuery,
  useGetMessagesQuery,
  useSendMessageMutation,
  useDeleteConversationMutation,
} = aiApi;
