package com.aerospace.api_gateway.filter;

import com.aerospace.api_gateway.service.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    // Public endpoints that don't require authentication
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/validate",
            "/actuator/health",
            "/ws"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Check if the request path is public
        boolean isPublicPath = PUBLIC_PATHS.stream().anyMatch(path::startsWith);
        if (isPublicPath) {
            log.debug("Public path accessed: {}", path);
            return chain.filter(exchange);
        }

        // Extract JWT token from Authorization header
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            log.warn("Invalid or expired JWT token for path: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // Extract user information from token
        String username = jwtUtil.extractUsername(token);
        Long userId = jwtUtil.extractUserId(token);
        List<String> roles = jwtUtil.extractRoles(token);

        // Add user information to request headers for downstream services
        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(r -> r.header("X-User-Id", String.valueOf(userId))
                        .header("X-Username", username)
                        .header("X-Roles", String.join(",", roles)))
                .build();

        log.debug("Authenticated user: {} with roles: {} accessing path: {}", username, roles, path);

        return chain.filter(mutatedExchange);
    }

    @Override
    public int getOrder() {
        // Run this filter first before routing
        return -100;
    }
}