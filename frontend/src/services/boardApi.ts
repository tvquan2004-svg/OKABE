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

export interface User {
  id: number;
  username: string;
  email: string;
  avatarUrl: string | null;
}

export interface Attachment {
  id: number;
  cardId: number;
  uploadedById: number;
  uploadedByUsername: string;
  filename: string;
  url: string;
  fileSize: number;
  mimeType: string;
  createdAt: string;
}

export interface CardItem {
  id: number;
  listId: number;
  title: string;
  description: string | null;
  position: number;
  dueDate: string | null;
  startDate: string | null;
  priority: string;
  isArchived: boolean;
  createdById: number;
  createdByName: string;
  createdAt: string;
  labels: Label[];
  checklists: Checklist[];
  members: User[];
  attachments: Attachment[];
}

export interface Activity {
  id: number;
  userId: number;
  username: string;
  avatarUrl: string | null;
  actionType: string;
  description: string | null;
  cardId?: number;
  createdAt: string;
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

export interface CardSearchParams {
  keyword?: string;
  assigneeIds?: number[];
  labelIds?: number[];
  priorities?: string[];
  dueDateFrom?: string;
  dueDateTo?: string;
  isOverdue?: boolean;
  page?: number;
  size?: number;
}

export interface PaginatedRes<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface BoardAnalyticsResponse {
  cardsByStatus: {
    listId: number;
    listName: string;
    total: number;
    overdue: number;
    completedThisWeek: number;
  }[];
  cardsByPriority: {
    priority: string;
    count: number;
  }[];
  cardsByMember: {
    userId: number;
    username: string;
    avatarUrl: string | null;
    assignedCount: number;
    overdueCount: number;
  }[];
  burndown: {
    date: string;
    remaining: number;
    completed: number;
  }[];
  avgCompletionDays: number;
  activityHeatmap: {
    date: string;
    count: number;
  }[];
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
    createBoard: builder.mutation<ApiRes<Board>, { workspaceId: number; name: string; description?: string; background?: string; templateId?: number }>({
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
      invalidatesTags: (_r, _e, { boardId, id }) => [{ type: 'Board', id: boardId }, { type: 'Activity', id }],
    }),
    moveCard: builder.mutation<ApiRes<CardItem>, { id: number; boardId: number; targetListId: number; position?: number }>({
      query: ({ id, boardId: _, ...body }) => ({ url: `/cards/${id}/move`, method: 'PUT', body }),
      invalidatesTags: (_r, _e, { boardId, id }) => [{ type: 'Board', id: boardId }, { type: 'Activity', id }],
    }),
    deleteCard: builder.mutation<ApiRes<void>, { id: number; boardId: number }>({
      query: ({ id }) => ({ url: `/cards/${id}`, method: 'DELETE' }),
      invalidatesTags: (_r, _e, { boardId }) => [{ type: 'Board', id: boardId }],
    }),

    // Phase 2: Checklists
    createChecklist: builder.mutation<ApiRes<Checklist>, { cardId: number; boardId: number; name: string }>({
      query: ({ cardId, name }) => ({ url: `/cards/${cardId}/checklists`, method: 'POST', body: { name } }),
      invalidatesTags: (_r, _e, { boardId, cardId }) => [{ type: 'Board', id: boardId }, { type: 'Activity', id: cardId }],
    }),
    updateChecklist: builder.mutation<ApiRes<Checklist>, { checklistId: number; boardId: number; cardId: number; name: string }>({
      query: ({ checklistId, name }) => ({ url: `/checklists/${checklistId}`, method: 'PUT', body: { name } }),
      invalidatesTags: (_r, _e, { boardId, cardId }) => [{ type: 'Board', id: boardId }, { type: 'Activity', id: cardId }],
    }),
    deleteChecklist: builder.mutation<ApiRes<void>, { checklistId: number; boardId: number; cardId: number }>({
      query: ({ checklistId }) => ({ url: `/checklists/${checklistId}`, method: 'DELETE' }),
      invalidatesTags: (_r, _e, { boardId, cardId }) => [{ type: 'Board', id: boardId }, { type: 'Activity', id: cardId }],
    }),
    createChecklistItem: builder.mutation<ApiRes<ChecklistItem>, { checklistId: number; boardId: number; cardId: number; content: string }>({
      query: ({ checklistId, content }) => ({ url: `/checklists/${checklistId}/items`, method: 'POST', body: { content } }),
      invalidatesTags: (_r, _e, { boardId, cardId }) => [{ type: 'Board', id: boardId }, { type: 'Activity', id: cardId }],
    }),
    updateChecklistItem: builder.mutation<ApiRes<ChecklistItem>, { itemId: number; boardId: number; cardId: number; content?: string; isCompleted?: boolean; position?: number }>({
      query: ({ itemId, ...body }) => ({ url: `/checklists/items/${itemId}`, method: 'PUT', body }),
      invalidatesTags: (_r, _e, { boardId, cardId }) => [{ type: 'Board', id: boardId }, { type: 'Activity', id: cardId }],
    }),
    deleteChecklistItem: builder.mutation<ApiRes<void>, { itemId: number; boardId: number; cardId: number }>({
      query: ({ itemId }) => ({ url: `/checklists/items/${itemId}`, method: 'DELETE' }),
      invalidatesTags: (_r, _e, { boardId, cardId }) => [{ type: 'Board', id: boardId }, { type: 'Activity', id: cardId }],
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
    updateLabel: builder.mutation<ApiRes<Label>, { id: number; boardId: number; body: Partial<Label> }>({
      query: ({ id, body }) => ({ url: `/labels/${id}`, method: 'PUT', body }),
      invalidatesTags: (_r, _e, { boardId }) => [{ type: 'Board', id: boardId }],
    }),
    deleteLabel: builder.mutation<ApiRes<void>, { id: number; boardId: number }>({
      query: ({ id }) => ({ url: `/labels/${id}`, method: 'DELETE' }),
      invalidatesTags: (_r, _e, { boardId }) => [{ type: 'Board', id: boardId }],
    }),
    addLabelToCard: builder.mutation<ApiRes<void>, { cardId: number; labelId: number; boardId: number }>({
      query: ({ cardId, labelId }) => ({ url: `/cards/${cardId}/labels/${labelId}`, method: 'POST' }),
      invalidatesTags: (_r, _e, { boardId }) => [{ type: 'Board', id: boardId }],
    }),
    removeLabelFromCard: builder.mutation<ApiRes<void>, { cardId: number; labelId: number; boardId: number }>({
      query: ({ cardId, labelId }) => ({ url: `/cards/${cardId}/labels/${labelId}`, method: 'DELETE' }),
      invalidatesTags: (_r, _e, { boardId }) => [{ type: 'Board', id: boardId }],
    }),

    // Phase 2: Member Assignment
    assignMember: builder.mutation<ApiRes<void>, { cardId: number; userId: number; boardId: number }>({
      query: ({ cardId, userId }) => ({ url: `/cards/${cardId}/members`, method: 'POST', body: { userId } }),
      invalidatesTags: (_r, _e, { boardId, cardId }) => [{ type: 'Board', id: boardId }, { type: 'Activity', id: cardId }],
    }),
    unassignMember: builder.mutation<ApiRes<void>, { cardId: number; userId: number; boardId: number }>({
      query: ({ cardId, userId }) => ({ url: `/cards/${cardId}/members/${userId}`, method: 'DELETE' }),
      invalidatesTags: (_r, _e, { boardId, cardId }) => [{ type: 'Board', id: boardId }, { type: 'Activity', id: cardId }],
    }),
    // Phase 2: Attachments
    uploadAttachment: builder.mutation<ApiRes<Attachment>, { cardId: number; boardId: number; file: File }>({
      query: ({ cardId, file }) => {
        const formData = new FormData();
        formData.append('file', file);
        return {
          url: `/cards/${cardId}/attachments`,
          method: 'POST',
          body: formData,
        };
      },
      invalidatesTags: (_r, _e, { boardId, cardId }) => [{ type: 'Board', id: boardId }, { type: 'Activity', id: cardId }],
    }),
    deleteAttachment: builder.mutation<ApiRes<void>, { attachmentId: number; boardId: number; cardId: number }>({
      query: ({ attachmentId }) => ({ url: `/attachments/${attachmentId}`, method: 'DELETE' }),
      invalidatesTags: (_r, _e, { boardId, cardId }) => [{ type: 'Board', id: boardId }, { type: 'Activity', id: cardId }],
    }),
    getCardActivities: builder.query<ApiRes<Activity[]>, number>({
      query: (cardId) => `/cards/${cardId}/activities`,
      providesTags: (_r, _e, cardId) => [{ type: 'Activity', id: cardId }],
    }),
    getBoardActivities: builder.query<ApiRes<Activity[]>, number>({
      query: (boardId) => `/boards/${boardId}/activities`,
      providesTags: (_r, _e, boardId) => [{ type: 'Board', id: boardId }],
    }),
    searchCards: builder.query<ApiRes<PaginatedRes<CardItem>>, { boardId: number; params: CardSearchParams }>({
      query: ({ boardId, params }) => {
        const queryParams = new URLSearchParams();
        if (params.keyword) queryParams.append('keyword', params.keyword);
        if (params.assigneeIds?.length) params.assigneeIds.forEach(id => queryParams.append('assigneeIds', id.toString()));
        if (params.labelIds?.length) params.labelIds.forEach(id => queryParams.append('labelIds', id.toString()));
        if (params.priorities?.length) params.priorities.forEach(p => queryParams.append('priorities', p));
        if (params.dueDateFrom) queryParams.append('dueDateFrom', params.dueDateFrom);
        if (params.dueDateTo) queryParams.append('dueDateTo', params.dueDateTo);
        if (params.isOverdue !== undefined) queryParams.append('isOverdue', params.isOverdue.toString());
        if (params.page !== undefined) queryParams.append('page', params.page.toString());
        if (params.size !== undefined) queryParams.append('size', params.size.toString());

        return `/boards/${boardId}/cards/search?${queryParams.toString()}`;
      },
      providesTags: (_r, _e, { boardId }) => [{ type: 'Board', id: boardId }],
    }),
    updateBoardBackground: builder.mutation<ApiRes<Board>, { id: number; type: 'COLOR' | 'IMAGE'; value?: string; file?: File }>({
      query: ({ id, type, value, file }) => {
        const formData = new FormData();
        formData.append('type', type);
        if (value) formData.append('value', value);
        if (file) formData.append('file', file);
        return {
          url: `/boards/${id}/background`,
          method: 'PATCH',
          body: formData,
        };
      },
      invalidatesTags: (_r, _e, { id }) => [{ type: 'Board', id }],
    }),
    // Phase 3: Comments
    getCardComments: builder.query<ApiRes<PaginatedRes<CommentResponse>>, { cardId: number; page?: number }>({
      query: ({ cardId, page = 0 }) => `/cards/${cardId}/comments?page=${page}&size=20`,
      providesTags: (_r, _e, { cardId }) => [{ type: 'Comment', id: cardId }],
    }),
    createComment: builder.mutation<ApiRes<CommentResponse>, { cardId: number; content: string }>({
      query: ({ cardId, content }) => ({ url: `/cards/${cardId}/comments`, method: 'POST', body: { content } }),
      invalidatesTags: (_r, _e, { cardId }) => [{ type: 'Comment', id: cardId }],
    }),
    updateComment: builder.mutation<ApiRes<CommentResponse>, { id: number; cardId: number; content: string }>({
      query: ({ id, content }) => ({ url: `/comments/${id}`, method: 'PUT', body: { content } }),
      invalidatesTags: (_r, _e, { cardId }) => [{ type: 'Comment', id: cardId }],
    }),
    deleteComment: builder.mutation<ApiRes<void>, { id: number; cardId: number }>({
      query: ({ id }) => ({ url: `/comments/${id}`, method: 'DELETE' }),
      invalidatesTags: (_r, _e, { cardId }) => [{ type: 'Comment', id: cardId }],
    }),
    // Archive/Restore
    archiveBoard: builder.mutation<ApiRes<Board>, number>({
      query: (id) => ({ url: `/boards/${id}/archive`, method: 'PUT' }),
      invalidatesTags: ['Board'],
    }),
    restoreBoard: builder.mutation<ApiRes<Board>, number>({
      query: (id) => ({ url: `/boards/${id}/restore`, method: 'PUT' }),
      invalidatesTags: ['Board'],
    }),
    getArchivedBoards: builder.query<ApiRes<Board[]>, number>({
      query: (workspaceId) => `/workspaces/${workspaceId}/boards?archived=true`,
      providesTags: ['Board'],
    }),
    archiveList: builder.mutation<ApiRes<TaskList>, { id: number; boardId: number }>({
      query: ({ id }) => ({ url: `/lists/${id}/archive`, method: 'PUT' }),
      invalidatesTags: (_r, _e, { boardId }) => [{ type: 'Board', id: boardId }],
    }),
    restoreList: builder.mutation<ApiRes<TaskList>, { id: number; boardId: number }>({
      query: ({ id }) => ({ url: `/lists/${id}/restore`, method: 'PUT' }),
      invalidatesTags: (_r, _e, { boardId }) => [{ type: 'Board', id: boardId }],
    }),
    getArchivedLists: builder.query<ApiRes<TaskList[]>, number>({
      query: (boardId) => `/boards/${boardId}/lists?archived=true`,
      providesTags: (_r, _e, boardId) => [{ type: 'Board', id: boardId }],
    }),
    archiveCard: builder.mutation<ApiRes<CardItem>, { id: number; boardId: number }>({
      query: ({ id }) => ({ url: `/cards/${id}/archive`, method: 'PUT' }),
      invalidatesTags: (_r, _e, { boardId, id }) => [{ type: 'Board', id: boardId }, { type: 'Activity', id }],
    }),
    restoreCard: builder.mutation<ApiRes<CardItem>, { id: number; boardId: number }>({
      query: ({ id }) => ({ url: `/cards/${id}/restore`, method: 'PUT' }),
      invalidatesTags: (_r, _e, { boardId, id }) => [{ type: 'Board', id: boardId }, { type: 'Activity', id }],
    }),
    getArchivedCards: builder.query<ApiRes<PaginatedRes<CardItem>>, { boardId: number; page?: number; size?: number }>({
      query: ({ boardId, page = 0, size = 20 }) => `/boards/${boardId}/cards/archived?page=${page}&size=${size}`,
      providesTags: (_r, _e, { boardId }) => [{ type: 'Board', id: boardId }],
    }),
    getBoardAnalytics: builder.query<ApiRes<BoardAnalyticsResponse>, number>({
      query: (boardId) => `/boards/${boardId}/analytics`,
      providesTags: (_r, _e, boardId) => [{ type: 'Board', id: boardId }],
    }),
  }),
});

export interface CommentResponse {
  id: number;
  cardId: number;
  author: User;
  content: string;
  isEdited: boolean;
  mentions: User[];
  createdAt: string;
  updatedAt: string;
}

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
  useUpdateChecklistMutation,
  useDeleteChecklistMutation,
  useCreateChecklistItemMutation,
  useUpdateChecklistItemMutation,
  useDeleteChecklistItemMutation,
  useCreateLabelMutation,
  useGetBoardLabelsQuery,
  useUpdateLabelMutation,
  useDeleteLabelMutation,
  useAddLabelToCardMutation,
  useRemoveLabelFromCardMutation,
  useAssignMemberMutation,
  useUnassignMemberMutation,
  useUploadAttachmentMutation,
  useDeleteAttachmentMutation,
  useGetCardActivitiesQuery,
  useGetBoardActivitiesQuery,
  useSearchCardsQuery,
  useUpdateBoardBackgroundMutation,
  useGetCardCommentsQuery,
  useCreateCommentMutation,
  useUpdateCommentMutation,
  useDeleteCommentMutation,
  useArchiveBoardMutation,
  useRestoreBoardMutation,
  useGetArchivedBoardsQuery,
  useArchiveListMutation,
  useRestoreListMutation,
  useGetArchivedListsQuery,
  useArchiveCardMutation,
  useRestoreCardMutation,
  useGetArchivedCardsQuery,
  useGetBoardAnalyticsQuery,
} = boardApi;

export const useGetBoardsByWorkspaceQuery = useGetBoardsQuery;
