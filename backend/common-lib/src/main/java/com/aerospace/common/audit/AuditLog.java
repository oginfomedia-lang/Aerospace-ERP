package com.aerospace.common.audit;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// BP-001 §4.11 — every create/update/approve/reject action across every service is
// recorded here: actor, IP, timestamp, and a full before/after snapshot (as JSON).
// previousValue is the prior AuditLog row's newValue for the same entityType+entityId —
// no separate "before" fetch needed, the trail is self-describing.
@Entity
@Table(name = "audit_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String entityType;

    private Long entityId;

    @Column(nullable = false)
    private String action;

    private String actor;
    private String ipAddress;

    @Column(columnDefinition = "TEXT")
    private String previousValue;

    @Column(columnDefinition = "TEXT")
    private String newValue;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }
}