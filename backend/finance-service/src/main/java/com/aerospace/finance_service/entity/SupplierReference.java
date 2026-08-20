package com.aerospace.finance_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Local, read-only mirror of a procurement-service Supplier — synced via Kafka
 * (SupplierRegisteredEvent), never written to directly by any finance-service
 * write path. Exists purely so journal lines / supplier payments can hold a real
 * foreign key instead of a free-text supplier reference.
 */
@Entity
@Table(name = "supplier_references")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupplierReference {

    // Deliberately NOT @GeneratedValue — this id is procurement-service's own
    // Supplier.id, taken verbatim from the event. That makes save() an idempotent
    // upsert-by-PK on Kafka redelivery (at-least-once semantics), and gives
    // JournalLine/SupplierPayment a natural join key.
    @Id
    private Long id;

    @Column(nullable = false, length = 200)
    private String supplierName;

    @Column(nullable = false, unique = true, length = 50)
    private String vendorCode;

    @Column(nullable = false, length = 100)
    private String registeredBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime syncedAt;

    @PrePersist
    void onCreate() { syncedAt = LocalDateTime.now(); }
}
