import { useState, useCallback, useRef } from 'react';
import { useLocation } from 'react-router-dom';
import { useSendMessageMutation } from '../services/aiApi';

interface LocalMessage {
  id: string;
  role: 'USER' | 'ASSISTANT';
  content: string;
  isLoading?: boolean;
}

interface UseAiChatReturn {
  messages: LocalMessage[];
  isLoading: boolean;
  conversationId: number | null;
  sendMessage: (text: string) => Promise<void>;
  clearConversation: () => void;
}

/**
 * Custom hook managing the AI chat state and message flow.
 * Handles optimistic UI updates and integrates with RTK Query.
 */
export function useAiChat(): UseAiChatReturn {
  const location = useLocation();
  const [messages, setMessages] = useState<LocalMessage[]>([]);
  const [conversationId, setConversationId] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const messageCounter = useRef(0);

  const [sendMessageMutation] = useSendMessageMutation();

  // Parse boardId from the URL if user is on a board page
  const getBoardIdFromUrl = useCallback((): number | undefined => {
    const match = location.pathname.match(/\/board\/(\d+)/);
    return match ? Number(match[1]) : undefined;
  }, [location.pathname]);

  const generateTempId = () => `temp-${++messageCounter.current}`;

  const sendMessage = useCallback(async (text: string) => {
    if (!text.trim() || isLoading) return;

    const userMsgId = generateTempId();
    const loadingMsgId = generateTempId();

    // Optimistic: add user message immediately
    setMessages((prev) => [
      ...prev,
      { id: userMsgId, role: 'USER', content: text },
      { id: loadingMsgId, role: 'ASSISTANT', content: '', isLoading: true },
    ]);
    setIsLoading(true);

    try {
      const result = await sendMessageMutation({
        message: text,
        conversationId: conversationId ?? undefined,
        boardId: getBoardIdFromUrl(),
      }).unwrap();

      const { data } = result;

      // Set conversation ID from response (first message auto-creates one)
      if (!conversationId && data.conversationId) {
        setConversationId(data.conversationId);
      }

      // Replace loading bubble with actual reply
      setMessages((prev) =>
        prev.map((m) =>
          m.id === loadingMsgId
            ? { id: loadingMsgId, role: 'ASSISTANT', content: data.reply, isLoading: false }
            : m
        )
      );
    } catch {
      setMessages((prev) =>
        prev.map((m) =>
          m.id === loadingMsgId
            ? {
                id: loadingMsgId,
                role: 'ASSISTANT',
                content: '❌ Xin lỗi, đã có lỗi xảy ra. Vui lòng thử lại!',
                isLoading: false,
              }
            : m
        )
      );
    } finally {
      setIsLoading(false);
    }
  }, [conversationId, isLoading, sendMessageMutation, getBoardIdFromUrl]);

  const clearConversation = useCallback(() => {
    setMessages([]);
    setConversationId(null);
  }, []);

  return { messages, isLoading, conversationId, sendMessage, clearConversation };
}
