package com.aerospace.inventory_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Part {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String skuCode; // ERP standard: unique internal code

    private String partType; // e.g., "RAW_MATERIAL", "CONSUMABLE"
    private String uom;      // Unit of Measure: "EA", "KG", "LTR"
    private String criticality; // LOW, MEDIUM, HIGH
    private Integer reorderLevel;

    // Planning levels (ITM-BR-006: minStock <= reorderLevel <= maxStock)
    private Integer minStock;
    private Integer maxStock;
    private Integer safetyStock;
    private Integer leadTimeDays;

    // Item lifecycle: ACTIVE, INACTIVE, DISCONTINUED (defaults to ACTIVE)
    @Builder.Default
    private String status = "ACTIVE";

    // Tracking flags (ITM-BR-002: batchRequired and serialRequired are mutually exclusive)
    // ColumnDefault is required here (not just @Builder.Default) so Hibernate's ALTER TABLE
    // on an existing, non-empty table can add these NOT NULL boolean columns without failing.
    @Builder.Default
    @ColumnDefault("true")
    @Column(nullable = false)
    private boolean batchRequired = true;

    @Builder.Default
    @ColumnDefault("false")
    @Column(nullable = false)
    private boolean serialRequired = false;

    // ITM-BR-003: if true, shelfLifeDays + expiry date on receipt become mandatory
    @Builder.Default
    @ColumnDefault("false")
    @Column(nullable = false)
    private boolean expiryTracking = false;

    private Integer shelfLifeDays;

    // GRN-FR-003: if true, GRN receipts route to PENDING_QA; if false, straight to APPROVED
    @Builder.Default
    @ColumnDefault("true")
    @Column(nullable = false)
    private boolean qcRequired = true;

    // Soft reference to procurement-service's Supplier (separate DB — no FK possible).
    // When set, an auto-raised shortage PO goes to this supplier; when null, the
    // shortage consumer falls back to the first CERTIFIED supplier (same fallback
    // procurement-service already uses for its low-stock auto-draft-PO).
    private Long preferredSupplierId;

    // Total quantity is now a derived value or managed via StockBalance logic
}
