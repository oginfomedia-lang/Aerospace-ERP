package com.aerospace.inventory_service.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Optional<AuditLog> findTopByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, Long entityId);
    List<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, Long entityId);
    List<AuditLog> findByEntityTypeOrderByTimestampDesc(String entityType);
    List<AuditLog> findByActorOrderByTimestampDesc(String actor);
    List<AuditLog> findAllByOrderByTimestampDesc();
}
