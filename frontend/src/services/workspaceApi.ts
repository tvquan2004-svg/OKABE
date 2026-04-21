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

export interface WorkspaceMember {
  userId: number;
  username: string;
  email: string;
  avatarUrl: string | null;
  role: string;
  joinedAt: string;
}

interface AddMemberRequest {
  email: string;
  role: string;
}

interface UpdateMemberRoleRequest {
  role: string;
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
    // Member Management
    getWorkspaceMembers: builder.query<ApiResponseWrapper<WorkspaceMember[]>, number>({
      query: (id) => `/workspaces/${id}/members`,
      providesTags: (_result, _error, id) => [{ type: 'WorkspaceMember', id }],
    }),
    addWorkspaceMember: builder.mutation<ApiResponseWrapper<WorkspaceMember>, { workspaceId: number; body: AddMemberRequest }>({
      query: ({ workspaceId, body }) => ({
        url: `/workspaces/${workspaceId}/members`,
        method: 'POST',
        body,
      }),
      invalidatesTags: (_result, _error, { workspaceId }) => [{ type: 'WorkspaceMember', id: workspaceId }],
    }),
    updateWorkspaceMemberRole: builder.mutation<ApiResponseWrapper<WorkspaceMember>, { workspaceId: number; memberId: number; body: UpdateMemberRoleRequest }>({
      query: ({ workspaceId, memberId, body }) => ({
        url: `/workspaces/${workspaceId}/members/${memberId}/role`,
        method: 'PUT',
        body,
      }),
      invalidatesTags: (_result, _error, { workspaceId }) => [{ type: 'WorkspaceMember', id: workspaceId }],
    }),
    removeWorkspaceMember: builder.mutation<ApiResponseWrapper<void>, { workspaceId: number; memberId: number }>({
      query: ({ workspaceId, memberId }) => ({
        url: `/workspaces/${workspaceId}/members/${memberId}`,
        method: 'DELETE',
      }),
      invalidatesTags: (_result, _error, { workspaceId }) => [{ type: 'WorkspaceMember', id: workspaceId }],
    }),
    acceptInvitation: builder.mutation<ApiResponseWrapper<void>, string>({
      query: (token) => ({
        url: `/workspaces/invitations/accept?token=${token}`,
        method: 'POST',
      }),
      invalidatesTags: ['Workspace'],
    }),
    rejectInvitation: builder.mutation<ApiResponseWrapper<void>, string>({
      query: (token) => ({
        url: `/workspaces/invitations/reject?token=${token}`,
        method: 'POST',
      }),
    }),
  }),
});

export const {
  useGetWorkspacesQuery,
  useGetWorkspaceQuery,
  useCreateWorkspaceMutation,
  useUpdateWorkspaceMutation,
  useDeleteWorkspaceMutation,
  useGetWorkspaceMembersQuery,
  useAddWorkspaceMemberMutation,
  useUpdateWorkspaceMemberRoleMutation,
  useRemoveWorkspaceMemberMutation,
  useAcceptInvitationMutation,
  useRejectInvitationMutation,
} = workspaceApi;
