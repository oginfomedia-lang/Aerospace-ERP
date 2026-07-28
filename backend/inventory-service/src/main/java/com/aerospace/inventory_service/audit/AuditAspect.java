package com.aerospace.inventory_service.audit;

import com.aerospace.inventory_service.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Optional;

// Fires on every controller method annotated @Audited — no changes needed to the
// service layer itself. Captures who (JWT-authenticated username), where from (IP),
// what happened (action/entityType from the annotation), and a before/after JSON
// snapshot (before = the last audit row for this entity, after = this call's result).
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @AfterReturning(pointcut = "@annotation(audited)", returning = "result")
    public void audit(Audited audited, Object result) {
        try {
            Object body = unwrap(result);
            Long entityId = extractId(body);
            String newValue = body != null ? objectMapper.writeValueAsString(body) : null;

            String previousValue = null;
            if (entityId != null) {
                previousValue = auditLogRepository
                        .findTopByEntityTypeAndEntityIdOrderByTimestampDesc(audited.entityType(), entityId)
                        .map(AuditLog::getNewValue)
                        .orElse(null);
            }

            auditLogRepository.save(AuditLog.builder()
                    .entityType(audited.entityType())
                    .entityId(entityId)
                    .action(audited.action())
                    .actor(currentActor())
                    .ipAddress(currentIp())
                    .previousValue(previousValue)
                    .newValue(newValue)
                    .build());
        } catch (Exception e) {
            // Audit logging must never break the actual business operation it's observing.
            log.error("[AUDIT] Failed to record audit entry for action={}, entityType={}: {}",
                    audited.action(), audited.entityType(), e.getMessage());
        }
    }

    // Unwraps ResponseEntity<ApiResponse<T>> down to T — every controller in this
    // service wraps its DTO in the common ApiResponse envelope.
    private Object unwrap(Object result) {
        Object body = result;
        if (body instanceof org.springframework.http.ResponseEntity<?> responseEntity) {
            body = responseEntity.getBody();
        }
        if (body instanceof ApiResponse<?> apiResponse) {
            body = apiResponse.getData();
        }
        return body;
    }

    // Every response DTO in this codebase exposes getId() — reflectively call it,
    // fall back to null (unauditable-by-id, still recorded) if it doesn't exist.
    private Long extractId(Object body) {
        if (body == null) return null;
        try {
            Method getId = body.getClass().getMethod("getId");
            Object id = getId.invoke(body);
            return id instanceof Long l ? l : (id instanceof Integer i ? i.longValue() : null);
        } catch (Exception e) {
            return null;
        }
    }

    private String currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "SYSTEM";
    }

    private String currentIp() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(ServletRequestAttributes.class::cast)
                .map(ServletRequestAttributes::getRequest)
                .map(this::resolveIp)
                .orElse(null);
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
