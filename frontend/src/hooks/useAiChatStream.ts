import { useState, useCallback, useRef, useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { useGetConversationsQuery, useGetMessagesQuery, useDeleteConversationMutation } from '../services/aiApi';

export interface LocalMessage {
  id: string;
  role: 'USER' | 'ASSISTANT' | 'SYSTEM';
  content: string;
  isLoading?: boolean;
}

export interface UseAiChatStreamReturn {
  messages: LocalMessage[];
  isLoading: boolean;
  conversationId: number | null;
  sendMessage: (text: string) => Promise<void>;
  clearConversation: () => void;
}

export function useAiChatStream(): UseAiChatStreamReturn {
  const location = useLocation();
  const [messages, setMessages] = useState<LocalMessage[]>([]);
  const [conversationId, setConversationId] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const messageCounter = useRef(0);

  const { data: conversationsData } = useGetConversationsQuery();
  const { data: messagesData, refetch: refetchMessages } = useGetMessagesQuery(conversationId!, {
    skip: !conversationId,
  });
  const [deleteConversationMutation] = useDeleteConversationMutation();

  // Load latest conversation on mount
  useEffect(() => {
    if (!conversationId && conversationsData?.data && conversationsData.data.length > 0) {
      const firstId = conversationsData.data[0]?.id;
      if (firstId) {
        setConversationId(firstId);
      }
    }
  }, [conversationsData, conversationId]);

  // Load messages when conversation ID is set
  useEffect(() => {
    if (messagesData?.data) {
      const history: LocalMessage[] = messagesData.data.map(m => ({
        id: String(m.id),
        role: m.role as 'USER' | 'ASSISTANT' | 'SYSTEM',
        content: m.content
      }));
      setMessages(history);
    }
  }, [messagesData]);

  const getBoardIdFromUrl = useCallback((): number | undefined => {
    const match = location.pathname.match(/\/board\/(\d+)/);
    return match ? Number(match[1]) : undefined;
  }, [location.pathname]);

  const generateTempId = () => `temp-${++messageCounter.current}`;

  const sendMessage = useCallback(async (text: string) => {
    if (!text.trim() || isLoading) return;

    const userMsgId = generateTempId();
    const assistantMsgId = generateTempId();

    setMessages((prev) => [
      ...prev,
      { id: userMsgId, role: 'USER', content: text },
      { id: assistantMsgId, role: 'ASSISTANT', content: '', isLoading: true },
    ]);
    setIsLoading(true);

    try {
      const token = localStorage.getItem('okabe_access_token');
      const response = await fetch(`${import.meta.env.VITE_API_URL || 'http://localhost:8080'}/api/v1/ai/chat/stream`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          message: text,
          conversationId: conversationId ?? undefined,
          boardId: getBoardIdFromUrl(),
        })
      });

      if (!response.ok) {
        if (response.status === 429) {
          throw new Error('429');
        }
        throw new Error('Network response was not ok');
      }

      if (!response.body) throw new Error('No readable stream');

      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let assistantContent = '';

      setMessages(prev => prev.map(m => m.id === assistantMsgId ? { ...m, isLoading: false } : m));

      let currentEvent = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        const chunk = decoder.decode(value, { stream: true });
        const lines = chunk.split('\n');

        for (const line of lines) {
          if (line.startsWith('event:')) {
            currentEvent = line.slice(6).trim();
          } else if (line.startsWith('data:')) {
            const data = line.slice(5).trim();
            
            if (currentEvent === 'token') {
              // Replace escaped newlines if any, or just append
              // Groq / SSE might send literal newlines, we append directly
              // SseEmitter encodes newlines if they are sent properly, but usually we just append text
              assistantContent += data === '\\n' ? '\n' : data;
              setMessages(prev => prev.map(m => m.id === assistantMsgId ? { ...m, content: assistantContent } : m));
            } else if (currentEvent === 'done') {
              if (data && !conversationId) {
                setConversationId(Number(data));
              }
            } else if (currentEvent === 'error') {
              assistantContent += '\n\n❌ ' + data;
              setMessages(prev => prev.map(m => m.id === assistantMsgId ? { ...m, content: assistantContent } : m));
            }
          } else if (line.trim() === '') {
            // End of event
            currentEvent = '';
          }
        }
      }
    } catch (e: unknown) {
      const errorText = e instanceof Error && e.message === '429' 
        ? '⏳ AI đang bận (Rate limit), vui lòng thử lại sau.' 
        : '❌ Xin lỗi, đã có lỗi kết nối xảy ra. Vui lòng thử lại!';
      
      setMessages((prev) =>
        prev.map((m) =>
          m.id === assistantMsgId
            ? { ...m, content: errorText, isLoading: false }
            : m
        )
      );
    } finally {
      setIsLoading(false);
      refetchMessages(); // To ensure consistency and get real IDs
    }
  }, [conversationId, isLoading, getBoardIdFromUrl, refetchMessages]);

  const clearConversation = useCallback(async () => {
    if (conversationId) {
      await deleteConversationMutation(conversationId);
    }
    setMessages([]);
    setConversationId(null);
  }, [conversationId, deleteConversationMutation]);

  return { messages, isLoading, conversationId, sendMessage, clearConversation };
}
