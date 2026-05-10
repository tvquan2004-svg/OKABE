import { apiSlice } from './apiSlice';

export interface TemplateCard {
  id: number;
  title: string;
  description: string | null;
  position: number;
}

export interface TemplateList {
  id: number;
  name: string;
  position: number;
  cards: TemplateCard[];
}

export interface BoardTemplate {
  id: number;
  name: string;
  description: string | null;
  isSystem: boolean;
  lists: TemplateList[] | null;
}

interface ApiRes<T> { success: boolean; data: T; message: string; }

export const templateApi = apiSlice.injectEndpoints({
  endpoints: (builder) => ({
    getTemplates: builder.query<ApiRes<BoardTemplate[]>, { workspaceId?: number }>({
      query: (params) => {
        const queryParams = new URLSearchParams();
        if (params.workspaceId) queryParams.append('workspaceId', params.workspaceId.toString());
        return `/templates?${queryParams.toString()}`;
      },
      providesTags: ['Template'],
    }),
    getTemplate: builder.query<ApiRes<BoardTemplate>, number>({
      query: (id) => `/templates/${id}`,
      providesTags: (_r, _e, id) => [{ type: 'Template', id }],
    }),
    deleteTemplate: builder.mutation<ApiRes<void>, number>({
      query: (id) => ({ url: `/templates/${id}`, method: 'DELETE' }),
      invalidatesTags: ['Template'],
    }),
    saveAsTemplate: builder.mutation<ApiRes<BoardTemplate>, { boardId: number; name: string; description?: string }>({
      query: ({ boardId, ...body }) => ({
        url: `/templates/boards/${boardId}/save`,
        method: 'POST',
        body,
      }),
      invalidatesTags: ['Template'],
    }),
  }),
});

export const {
  useGetTemplatesQuery,
  useGetTemplateQuery,
  useDeleteTemplateMutation,
  useSaveAsTemplateMutation,
} = templateApi;
