export interface ConversationResponse {
  id: number;
  title: string;
  boardId?: number;
  workspaceId?: number;
  createdAt: string;
  updatedAt: string;
  messages?: MessageResponse[];
}

export interface MessageResponse {
  id: number;
  role: 'USER' | 'ASSISTANT' | 'SYSTEM';
  content: string;
  createdAt: string;
}

export interface ChatResponse {
  conversationId: number;
  messageId: number;
  reply: string;
  createdAt: string;
}

export interface ChatRequest {
  message: string;
  conversationId?: number;
  boardId?: number;
  workspaceId?: number;
}

export interface SubtaskSuggestion {
  title: string;
  estimatedHours: number;
}

export interface PrioritySuggestion {
  suggestedPriority: string;
  score: number;
  reason: string;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
}
