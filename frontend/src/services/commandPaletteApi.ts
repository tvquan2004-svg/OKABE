import { apiSlice } from './apiSlice';

interface ApiRes<T> { success: boolean; data: T; message: string; }

export interface SearchResultItem {
  id: string;
  type: 'board' | 'card' | 'member' | 'workspace' | 'command';
  title: string;
  subtitle: string;
  breadcrumb: string;
  url: string | null;
  icon: string;
}

interface CommandRequest {
  command: string;
}

interface CommandResponse {
  type: string;
  message: string;
  data: Record<string, unknown>;
}

export const commandPaletteApi = apiSlice.injectEndpoints({
  endpoints: (builder) => ({
    globalSearch: builder.query<ApiRes<SearchResultItem[]>, string>({
      query: (q) => `/search/global?q=${encodeURIComponent(q)}`,
    }),
    executeCommand: builder.mutation<ApiRes<CommandResponse>, string>({
      query: (command) => ({
        url: '/commands/execute',
        method: 'POST',
        body: { command } satisfies CommandRequest,
      }),
    }),
  }),
});

export const {
  useGlobalSearchQuery,
  useLazyGlobalSearchQuery,
  useExecuteCommandMutation,
} = commandPaletteApi;
