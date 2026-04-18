import { apiSlice } from './apiSlice';

interface WorkspaceOwner {
  id: number;
  username: string;
  email: string;
  avatarUrl: string | null;
}

export interface Workspace {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  owner: WorkspaceOwner;
  currentUserRole: string;
  memberCount: number;
  createdAt: string;
}

interface CreateWorkspaceRequest {
  name: string;
  description?: string;
}

interface UpdateWorkspaceRequest {
  name?: string;
  description?: string;
}

interface ApiResponseWrapper<T> {
  success: boolean;
  data: T;
  message: string;
}

export const workspaceApi = apiSlice.injectEndpoints({
  endpoints: (builder) => ({
    getWorkspaces: builder.query<ApiResponseWrapper<Workspace[]>, void>({
      query: () => '/workspaces',
      providesTags: ['Workspace'],
    }),
    getWorkspace: builder.query<ApiResponseWrapper<Workspace>, number>({
      query: (id) => `/workspaces/${id}`,
      providesTags: (_result, _error, id) => [{ type: 'Workspace', id }],
    }),
    createWorkspace: builder.mutation<ApiResponseWrapper<Workspace>, CreateWorkspaceRequest>({
      query: (body) => ({
        url: '/workspaces',
        method: 'POST',
        body,
      }),
      invalidatesTags: ['Workspace'],
    }),
    updateWorkspace: builder.mutation<ApiResponseWrapper<Workspace>, { id: number; body: UpdateWorkspaceRequest }>({
      query: ({ id, body }) => ({
        url: `/workspaces/${id}`,
        method: 'PUT',
        body,
      }),
      invalidatesTags: (_result, _error, { id }) => [{ type: 'Workspace', id }, 'Workspace'],
    }),
    deleteWorkspace: builder.mutation<ApiResponseWrapper<void>, number>({
      query: (id) => ({
        url: `/workspaces/${id}`,
        method: 'DELETE',
      }),
      invalidatesTags: ['Workspace'],
    }),
  }),
});

export const {
  useGetWorkspacesQuery,
  useGetWorkspaceQuery,
  useCreateWorkspaceMutation,
  useUpdateWorkspaceMutation,
  useDeleteWorkspaceMutation,
} = workspaceApi;
