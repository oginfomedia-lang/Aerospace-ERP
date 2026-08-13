package com.aerospace.notification_service.service;

import com.aerospace.notification_service.entity.NotificationLog;
import com.aerospace.notification_service.util.NotificationRoleMapping;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private final SimpUserRegistry simpUserRegistry; // auto-provided by Spring — tracks who's connected

    // actorUsername may be null (most events have no single "person who did this" —
    // see NotificationServiceImpl) — when set, that person is skipped, since they
    // already know, they just did it themselves.
    public void broadcast(NotificationLog notification, String actorUsername) {
        if (notification.getType() == null) return;

        Set<String> relevantRoles = NotificationRoleMapping.rolesFor(notification.getType());

        for (SimpUser user : simpUserRegistry.getUsers()) {
            String username = user.getName();
            if (username.equals(actorUsername)) continue;

            Set<String> userRoles = rolesOf(user);
            if (Collections.disjoint(userRoles, relevantRoles)) continue;

            messagingTemplate.convertAndSendToUser(username, "/queue/notifications", notification);
            log.debug("[WS] Delivered → user={} type={}", username, notification.getType());
        }
    }

    private Set<String> rolesOf(SimpUser user) {
        if (user.getPrincipal() instanceof Authentication auth) {
            return auth.getAuthorities().stream()
                    .map(a -> a.getAuthority().replaceFirst("^ROLE_", ""))
                    .collect(Collectors.toSet());
        }
        return Set.of();
    }
}
