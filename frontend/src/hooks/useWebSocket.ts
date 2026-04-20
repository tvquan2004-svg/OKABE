import { useEffect, useRef, useCallback } from 'react';
import { Client, IFrame, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useAppSelector } from './useRedux';

export interface WebSocketMessage {
  type: string;
  boardId?: number;
  payload: any;
  actorId?: number;
  timestamp: string;
}

interface UseWebSocketOptions {
  onMessage?: (message: WebSocketMessage) => void;
  topics?: string[];
}

export const useWebSocket = (options: UseWebSocketOptions = {}) => {
  const { onMessage, topics = [] } = options;
  const { accessToken, user } = useAppSelector((state) => state.auth);
  const clientRef = useRef<Client | null>(null);

  const connect = useCallback(() => {
    if (!accessToken || !user) return;

    // Use absolute URL for WebSocket if defined in env, otherwise relative
    const socketUrl = import.meta.env.VITE_WS_URL || '/ws';
    
    const client = new Client({
      webSocketFactory: () => new SockJS(socketUrl),
      connectHeaders: {
        Authorization: `Bearer ${accessToken}`,
      },
      debug: (str: string) => {
        if (import.meta.env.DEV) console.log('STOMP: ' + str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    client.onConnect = (frame: IFrame) => {
      if (import.meta.env.DEV) console.log('Connected to WebSocket: ' + frame.body);
      
      // Subscribe to personal notifications
      client.subscribe(`/user/${user.id}/queue/notifications`, (message: IMessage) => {
        if (onMessage) onMessage(JSON.parse(message.body));
      });

      // Subscribe to provided topics (e.g., board topics)
      topics.forEach((topic) => {
        client.subscribe(topic, (message: IMessage) => {
          if (onMessage) onMessage(JSON.parse(message.body));
        });
      });
    };

    client.onStompError = (frame: IFrame) => {
      console.error('STOMP error', frame.headers['message']);
      console.error('STOMP details', frame.body);
    };

    client.activate();
    clientRef.current = client;
  }, [accessToken, user, topics, onMessage]);

  const disconnect = useCallback(() => {
    if (clientRef.current) {
      clientRef.current.deactivate();
      clientRef.current = null;
    }
  }, []);

  useEffect(() => {
    connect();
    return () => disconnect();
  }, [connect, disconnect]);

  return {
    client: clientRef.current,
    isConnected: clientRef.current?.connected || false,
  };
};
