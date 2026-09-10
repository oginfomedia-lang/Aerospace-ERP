// hooks/useNotificationSocket.ts
// Connects to ws://localhost:8083/ws via SockJS + STOMP, proving who you are by
// sending your login token (JWT) as a connect header — the backend validates it
// and attaches your real identity to the connection (see
// WebSocketAuthChannelInterceptor.java on the backend). From then on, the server
// decides what you're allowed to see; this hook just listens on your own personal
// mailbox (/user/queue/notifications), which Spring routes automatically once the
// connection is authenticated — no more figuring out which "rooms" to join.
//
// Install deps if not already present:
//   npm install @stomp/stompjs sockjs-client
//   npm install --save-dev @types/sockjs-client

import { useEffect, useRef } from 'react';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { NotificationLog } from '../types/notification';
import { NOTIFICATION_WS_URL } from '../config/apiConfig';

interface Options {
  token: string | null; // the logged-in user's JWT — reconnects if this changes
  onMessage: (n: NotificationLog) => void;
  onConnect?: () => void;
  onDisconnect?: () => void;
}

export function useNotificationSocket({ token, onMessage, onConnect, onDisconnect }: Options) {
  const clientRef = useRef<Client | null>(null);
  const onMessageRef = useRef(onMessage);
  const onConnectRef = useRef(onConnect);
  const onDisconnectRef = useRef(onDisconnect);

  // Keep refs current without triggering reconnect
  useEffect(() => { onMessageRef.current = onMessage; }, [onMessage]);
  useEffect(() => { onConnectRef.current = onConnect; }, [onConnect]);
  useEffect(() => { onDisconnectRef.current = onDisconnect; }, [onDisconnect]);

  useEffect(() => {
    if (!token) return; // not logged in — nothing to connect for

    const client = new Client({
      webSocketFactory: () => new SockJS(NOTIFICATION_WS_URL),
      reconnectDelay: 5000,
      connectHeaders: { Authorization: `Bearer ${token}` },

      onConnect: () => {
        onConnectRef.current?.();
        client.subscribe('/user/queue/notifications', (msg: IMessage) => {
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
  }, [token]); // reconnect (with the fresh token) if it changes, e.g. re-login as someone else

  return { client: clientRef.current };
}
