// hooks/useNotificationSocket.ts
// Connects to ws://localhost:8083/ws via SockJS + STOMP
// Subscribes to /topic/notifications — same channel WebSocketService.broadcast() pushes to
//
// Install deps if not already present:
//   npm install @stomp/stompjs sockjs-client
//   npm install --save-dev @types/sockjs-client

import { useEffect, useRef, useCallback } from 'react';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { NotificationLog } from '../types/notification';

interface Options {
  onMessage: (n: NotificationLog) => void;
  onConnect?: () => void;
  onDisconnect?: () => void;
}

export function useNotificationSocket({ onMessage, onConnect, onDisconnect }: Options) {
  const clientRef = useRef<Client | null>(null);
  const onMessageRef = useRef(onMessage);
  const onConnectRef = useRef(onConnect);
  const onDisconnectRef = useRef(onDisconnect);

  // Keep refs current without triggering reconnect
  useEffect(() => { onMessageRef.current = onMessage; }, [onMessage]);
  useEffect(() => { onConnectRef.current = onConnect; }, [onConnect]);
  useEffect(() => { onDisconnectRef.current = onDisconnect; }, [onDisconnect]);

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8083/ws'),
      reconnectDelay: 5000,

      onConnect: () => {
        onConnectRef.current?.();
        client.subscribe('/topic/notifications', (msg: IMessage) => {
          try {
            const notification: NotificationLog = JSON.parse(msg.body);
            onMessageRef.current(notification);
          } catch (e) {
            console.error('[WS] Failed to parse notification:', e);
          }
        });
      },

      onDisconnect: () => onDisconnectRef.current?.(),
      onStompError: (frame) => console.error('[WS] STOMP error:', frame.headers['message']),
    });

    client.activate();
    clientRef.current = client;

    return () => { client.deactivate(); };
  }, []); // ← empty deps — connect once, use refs for callbacks

  return { client: clientRef.current };
}