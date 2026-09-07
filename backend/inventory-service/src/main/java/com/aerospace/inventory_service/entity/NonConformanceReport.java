package com.aerospace.inventory_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// Generated automatically whenever QA rejects a batch (see PartServiceImpl.processQADecision).
// A REJECTED StockBalance stays blocked in the ledger until this NCR is dispositioned —
// disposition is the only path that actually clears it out (or moves it to APPROVED, for
// USE_AS_IS under special approval).
@Entity
@Table(name = "non_conformance_reports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NonConformanceReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String ncrNumber;

    private Long partId;
    private String partName;
    private String batchNumber;
    private Integer quantity;

    // Soft reference to the procurement-service PO this rejected batch was received
    // against — null when the batch wasn't traceable to a PO (e.g. manual stock add).
    private String poNumber;

    @Column(length = 1000)
    private String reason; // QA remarks captured at rejection time

    // Soft reference to the FAI (compliance-service) whose NON_CONFORMING result
    // auto-created this NCR (InventoryConsumer.consumeFaiResult) — null for NCRs raised
    // any other way. Used to dedupe against Kafka at-least-once redelivery of the same
    // FAI-completed event.
    private String sourceFaiNumber;

    @Enumerated(EnumType.STRING)
    private NcrStatus status;

    @Enumerated(EnumType.STRING)
    private DispositionType dispositionType; // null until dispositioned

    private String dispositionRemarks;
    private String dispositionedBy;

    private LocalDateTime createdAt;
    private LocalDateTime dispositionedAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = NcrStatus.OPEN;
    }
}
