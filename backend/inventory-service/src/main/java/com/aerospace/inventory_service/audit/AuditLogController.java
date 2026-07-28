package com.aerospace.inventory_service.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Read-only audit trail viewer — BP-001 §4.11. Restricted to management.
@RestController
@RequestMapping("/api/v1/inventory/audit-log")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    public ResponseEntity<List<AuditLog>> getAll(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) String actor) {
        if (entityType != null && entityId != null) {
            return ResponseEntity.ok(auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId));
        }
        if (entityType != null) {
            return ResponseEntity.ok(auditLogRepository.findByEntityTypeOrderByTimestampDesc(entityType));
        }
        if (actor != null) {
            return ResponseEntity.ok(auditLogRepository.findByActorOrderByTimestampDesc(actor));
        }
        return ResponseEntity.ok(auditLogRepository.findAllByOrderByTimestampDesc());
    }
}
