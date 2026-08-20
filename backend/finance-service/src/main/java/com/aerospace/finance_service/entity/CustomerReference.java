package com.aerospace.finance_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Local, read-only mirror of a sales-service Customer — synced via Kafka
 * (CustomerRegisteredEvent/CustomerUpdatedEvent), never written to directly by any
 * finance-service write path. Mirrors SupplierReference exactly.
 */
@Entity
@Table(name = "customer_references")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CustomerReference {

    // Deliberately NOT @GeneratedValue — this is sales-service's own Customer.id,
    // taken verbatim from the event, making save() an idempotent upsert-by-PK.
    @Id
    private Long id;

    @Column(nullable = false, length = 200)
    private String customerName;

    @Column(nullable = false, unique = true, length = 50)
    private String customerCode;

    // "ACTIVE" | "INACTIVE" | "ON_HOLD" — stored as a plain String, not sales-service's
    // CustomerStatus enum, to avoid a compile-time dependency across services.
    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false, length = 100)
    private String registeredBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime syncedAt;

    @PrePersist
    void onCreate() { syncedAt = LocalDateTime.now(); }
}
