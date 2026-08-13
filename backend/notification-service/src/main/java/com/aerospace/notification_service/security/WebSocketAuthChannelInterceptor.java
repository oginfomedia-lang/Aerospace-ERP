package com.aerospace.notification_service.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

// Runs on every inbound STOMP frame. On CONNECT, reads the JWT the frontend sent as
// an "Authorization" header (see useNotificationSocket.ts) and validates it with
// the exact same JwtUtil every REST endpoint in this service already trusts. Once
// validated, the real logged-in identity is attached to this WebSocket session —
// from this point on, Spring knows exactly who this connection belongs to, and
// WebSocketService uses that to decide who's actually allowed to receive what.
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            String token = (authHeader != null && authHeader.startsWith("Bearer "))
                    ? authHeader.substring(7) : null;

            if (token == null || !jwtUtil.validateToken(token)) {
                log.warn("[WS] Rejected a connection attempt with a missing or invalid token");
                throw new AccessDeniedException("Missing or invalid token");
            }

            String username = jwtUtil.extractUsername(token);
            List<String> roles = jwtUtil.extractRoles(token);
            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());

            accessor.setUser(new UsernamePasswordAuthenticationToken(username, null, authorities));
            log.debug("[WS] Authenticated WebSocket connection for user={}", username);
        }

        return message;
    }
}
