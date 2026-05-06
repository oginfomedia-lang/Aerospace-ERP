package com.aerospace.notification_service.service;

import com.aerospace.notification_service.entity.NotificationLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Pushes to ALL connected frontend clients.
     * Subscribe: /topic/notifications
     */
    public void broadcast(NotificationLog notification) {
        messagingTemplate.convertAndSend("/topic/notifications", notification);
        log.debug("[WS] Broadcast → /topic/notifications | type={} partId={}",
                notification.getType(), notification.getPartId());

        // Also push to part-specific channel
        if (notification.getPartId() != null) {
            String dest = "/topic/notifications/" + notification.getPartId();
            messagingTemplate.convertAndSend(dest, notification);
            log.debug("[WS] Broadcast → {} ", dest);
        }

        // Also push to type-specific channel
        if (notification.getType() != null) {
            String dest = "/topic/notifications/type/" + notification.getType().name();
            messagingTemplate.convertAndSend(dest, notification);
            log.debug("[WS] Broadcast → {}", dest);
        }
    }
}