package com.aerospace.inventory_service.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

// Real (part_id, batch_number, status, location) rows are legitimately non-unique
// without `location` — the same batch can sit split across more than one physical
// location (confirmed against live data: part_id=1/BATCH-2026-01/APPROVED had two
// rows, one per warehouse location, before this constraint was added). Constraining
// on just (part_id, batch_number, status) would have broken that and rejected valid
// multi-location stock.
@Entity
@Table(name = "stock_balance",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_stock_balance_part_batch_status_location",
                columnNames = {"part_id", "batch_number", "status", "location"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockBalance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Deliberately kept as a bare Long rather than a @ManyToOne(Part) relationship —
    // converting it would touch every call site across PartServiceImpl/NcrServiceImpl/
    // the Kafka consumers that currently treat it as a plain id, which is a much larger,
    // riskier change than this pass is scoped for. No DB-level FK exists on this column;
    // service-layer code is what's relied on to only ever write a real Part's id here.
    private Long partId; // Links to Part Master

    @Column(nullable = false)
    private String batchNumber; // Critical for Aerospace traceability

    @Column(nullable = false)
    private Integer quantity; // The quantity for THIS specific batch/status combo

    @Enumerated(EnumType.STRING)
    private StockStatus status; // PENDING_QA (Blocked), APPROVED (Available), REJECTED

    private LocalDateTime expiryDate;//expiry tracking
    private String uom; // Unit of Measure, e.g., "EA", "KG", "LTR"

    private String location; // e.g., "WH1-ZONE-A-BIN12"

    // Soft reference to the procurement-service PO this batch was received against —
    // null for stock added outside a GRN (manual adjustments, initial stock entry).
    private String poNumber;

    // Soft reference to the production-service order this finished-goods batch was
    // credited from (ProductionCompletionConsumer). Null for stock that didn't come from
    // a production completion. Used to dedupe against Kafka at-least-once redelivery of
    // the same PRODUCTION_ORDER_COMPLETED event — see existsBySourceProductionOrderRef.
    private String sourceProductionOrderRef;

    private LocalDateTime lastUpdated;

    @PreUpdate
    @PrePersist
    public void updateTimestamp() {
        this.lastUpdated = LocalDateTime.now();
    }
}
