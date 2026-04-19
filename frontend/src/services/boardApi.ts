import { apiSlice } from './apiSlice';

export interface Label {
  id: number;
  boardId: number;
  name: string;
  color: string;
}

export interface ChecklistItem {
  id: number;
  checklistId: number;
  content: string;
  isCompleted: boolean;
  position: number;
}

export interface Checklist {
  id: number;
  cardId: number;
  name: string;
  position: number;
  items: ChecklistItem[];
}

export interface CardItem {
  id: number;
  listId: number;
  title: string;
  description: string | null;
  position: number;
  dueDate: string | null;
  priority: string;
  isArchived: boolean;
  createdById: number;
  createdByName: string;
  createdAt: string;
  labels: Label[];
  checklists: Checklist[];
}

export interface TaskList {
  id: number;
  boardId: number;
  name: string;
  position: number;
  cards: CardItem[];
}

export interface Board {
  id: number;
  workspaceId: number;
  name: string;
  description: string | null;
  position: number;
  background: string | null;
  isStarred: boolean;
  isArchived: boolean;
  listCount: number;
  totalCards: number;
  createdAt: string;
  lists: TaskList[] | null;
}

interface ApiRes<T> { success: boolean; data: T; message: string; }

export const boardApi = apiSlice.injectEndpoints({
  endpoints: (builder) => ({
    getBoards: builder.query<ApiRes<Board[]>, number>({
      query: (workspaceId) => `/workspaces/${workspaceId}/boards`,
      providesTags: ['Board'],
    }),
    getBoard: builder.query<ApiRes<Board>, number>({
      query: (id) => `/boards/${id}`,
      providesTags: (_r, _e, id) => [{ type: 'Board', id }],
    }),
    createBoard: builder.mutation<ApiRes<Board>, { workspaceId: number; name: string; description?: string; background?: string }>({
      query: ({ workspaceId, ...body }) => ({ url: `/workspaces/${workspaceId}/boards`, method: 'POST', body }),
      invalidatesTags: ['Board'],
    }),
    updateBoard: builder.mutation<ApiRes<Board>, { id: number; body: Partial<Board> }>({
      query: ({ id, body }) => ({ url: `/boards/${id}`, method: 'PUT', body }),
      invalidatesTags: (_r, _e, { id }) => [{ type: 'Board', id }, 'Board'],
    }),
    reorderBoards: builder.mutation<ApiRes<void>, { workspaceId: number; orderedIds: number[] }>({
      query: ({ workspaceId, orderedIds }) => ({
        url: `/workspaces/${workspaceId}/boards/reorder`,
        method: 'PUT',
        body: { orderedIds },
      }),
      invalidatesTags: ['Board'],
    }),
    deleteBoard: builder.mutation<ApiRes<void>, number>({
      query: (id) => ({ url: `/boards/${id}`, method: 'DELETE' }),
      invalidatesTags: ['Board'],
    }),
    // Lists
    createList: builder.mutation<ApiRes<TaskList>, { boardId: number; name: string }>({
      query: ({ boardId, name }) => ({ url: `/boards/${boardId}/lists`, method: 'POST', body: { name } }),
      invalidatesTags: (_r, _e, { boardId }) => [{ type: 'Board', id: boardId }],
    }),
    updateList: builder.mutation<ApiRes<TaskList>, { id: number; boardId: number; body: { name?: string; isArchived?: boolean } }>({
      query: ({ id, body }) => ({ url: `/lists/${id}`, method: 'PUT', body }),
      invalidatesTags: (_r, _e, { boardId }) => [{ type: 'Board', id: boardId }],
    }),
    reorderLists: builder.mutation<ApiRes<void>, { boardId: number; orderedIds: number[] }>({
      query: ({ boardId, orderedIds }) => ({ url: `/boards/${boardId}/lists/reorder`, method: 'PUT', body: { orderedIds } }),
      invalidatesTags: (_r, _e, { boardId }) => [{ type: 'Board', id: boardId }],
    }),
    deleteList: builder.mutation<ApiRes<void>, { id: number; boardId: number }>({
      query: ({ id }) => ({ url: `/lists/${id}`, method: 'DELETE' }),
      invalidatesTags: (_r, _e, { boardId }) => [{ type: 'Board', id: boardId }],
    }),
    // Cards
    createCard: builder.mutation<ApiRes<CardItem>, { listId: number; boardId: number; title: string; description?: string; priority?: string }>({
      query: ({ listId, boardId: _, ...body }) => ({ url: `/lists/${listId}/cards`, method: 'POST', body }),
      invalidatesTags: (_r, _e, { boardId }) => [{ type: 'Board', id: boardId }],
    }),
    updateCard: builder.mutation<ApiRes<CardItem>, { id: number; boardId: number; body: Partial<CardItem> }>({
      query: ({ id, body }) => ({ url: `/cards/${id}`, method: 'PUT', body }),
      invalidatesTags: (_r, _e, { boardId }) => [{ type: 'Board', id: boardId }],
    }),
    moveCard: builder.mutation<ApiRes<CardItem>, { id: number; boardId: number; targetListId: number; newPosition: number }>({
      query: ({ id, boardId: _, ...body }) => ({ url: `/cards/${id}/move`, method: 'PUT', body }),
      invalidatesTags: (_r, _e, { boardId }) => [{ type: 'Board', id: boardId }],
    }),
    deleteCard: builder.mutation<ApiRes<void>, { id: number; boardId: number }>({
      query: ({ id }) => ({ url: `/cards/${id}`, method: 'DELETE' }),
      invalidatesTags: (_r, _e, { boardId }) => [{ type: 'Board', id: boardId }],
    }),

    // Phase 2: Checklists
    createChecklist: builder.mutation<ApiRes<Checklist>, { cardId: number; boardId: number; name: string }>({
      query: ({ cardId, name }) => ({ url: `/cards/${cardId}/checklists`, method: 'POST', body: { name } }),
      invalidatesTags: (_r, _e, { boardId }) => [{ type: 'Board', id: boardId }],
    }),
    createChecklistItem: builder.mutation<ApiRes<ChecklistItem>, { checklistId: number; boardId: number; content: string }>({
      query: ({ checklistId, content }) => ({ url: `/checklists/${checklistId}/items`, method: 'POST', body: { content } }),
      invalidatesTags: (_r, _e, { boardId }) => [{ type: 'Board', id: boardId }],
    }),
    updateChecklistItem: builder.mutation<ApiRes<ChecklistItem>, { itemId: number; boardId: number; body: { content?: string; isCompleted?: boolean; position?: number } }>({
      query: ({ itemId, body }) => ({ url: `/checklists/items/${itemId}`, method: 'PUT', body }),
      invalidatesTags: (_r, _e, { boardId }) => [{ type: 'Board', id: boardId }],
    }),

    // Phase 2: Labels
    createLabel: builder.mutation<ApiRes<Label>, { boardId: number; name?: string; color: string }>({
      query: ({ boardId, ...body }) => ({ url: `/boards/${boardId}/labels`, method: 'POST', body }),
      invalidatesTags: (_r, _e, { boardId }) => [{ type: 'Board', id: boardId }],
    }),
    getBoardLabels: builder.query<ApiRes<Label[]>, number>({
      query: (boardId) => `/boards/${boardId}/labels`,
      providesTags: (_r, _e, boardId) => [{ type: 'Board', id: boardId }],
    }),
    addLabelToCard: builder.mutation<ApiRes<void>, { cardId: number; labelId: number; boardId: number }>({
      query: ({ cardId, labelId }) => ({ url: `/cards/${cardId}/labels/${labelId}`, method: 'POST' }),
      invalidatesTags: (_r, _e, { boardId }) => [{ type: 'Board', id: boardId }],
    }),
    removeLabelFromCard: builder.mutation<ApiRes<void>, { cardId: number; labelId: number; boardId: number }>({
      query: ({ cardId, labelId }) => ({ url: `/cards/${cardId}/labels/${labelId}`, method: 'DELETE' }),
      invalidatesTags: (_r, _e, { boardId }) => [{ type: 'Board', id: boardId }],
    }),
  }),
});

export const {
  useGetBoardsQuery,
  useGetBoardQuery,
  useCreateBoardMutation,
  useUpdateBoardMutation,
  useReorderBoardsMutation,
  useDeleteBoardMutation,
  useCreateListMutation,
  useUpdateListMutation,
  useReorderListsMutation,
  useDeleteListMutation,
  useCreateCardMutation,
  useUpdateCardMutation,
  useMoveCardMutation,
  useDeleteCardMutation,
  // Phase 2
  useCreateChecklistMutation,
  useCreateChecklistItemMutation,
  useUpdateChecklistItemMutation,
  useCreateLabelMutation,
  useGetBoardLabelsQuery,
  useAddLabelToCardMutation,
  useRemoveLabelFromCardMutation,
} = boardApi;
