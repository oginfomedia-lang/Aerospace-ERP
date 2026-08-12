package com.aerospace.production_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// A lightweight, self-contained non-conformance record for an IN-PROCESS (shop-floor)
// reject — created when completing a routing stage with a non-zero scrap/rework
// quantity. Deliberately independent of inventory-service's vendor-facing
// NonConformanceReport/DispositionType (RETURN_TO_VENDOR/USE_AS_IS don't apply to a
// shop-floor reject, and there's no StockBalance/PENDING_QA row to hang it off of) —
// this never calls qa-service and never touches inventory-service.
@Entity
@Table(name = "shop_floor_ncrs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopFloorNcr {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productionOrderId;

    // Null for a FINISHED_GOODS_REJECT — that disposition is order-level (recorded at
    // "Send to QA"), not tied to any one BOM line.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_material_id", nullable = true)
    @ToString.Exclude
    private ProductionMaterial productionMaterial;

    // Null for a FINISHED_GOODS_REJECT raised on the legacy no-routing IN_PRODUCTION path,
    // where there's no order-level "Final Assembly" stage log to attach to.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_log_id", nullable = true)
    @ToString.Exclude
    private ProductionStageLog stageLog;

    private Integer rejectedQuantity;

    @Enumerated(EnumType.STRING)
    private ShopFloorDisposition disposition;

    private String reasonCode;
    private String description;

    private String reportedBy;
    private LocalDateTime reportedAt;

    // Set only for REWORK — points at the new same-sequence redo attempt row.
    private Long reworkStageLogId;

    // Not @Builder.Default'd to NOT NULL at the column level — this table already has
    // rows from before this field existed, and ddl-auto:update silently fails to backfill
    // a NOT NULL column on a non-empty table (hit this exact bug twice already this
    // session). Existing rows need a manual `UPDATE shop_floor_ncrs SET status='OPEN'
    // WHERE status IS NULL` after deploy. REWORK NCRs auto-resolve when their
    // reworkStageLogId's stage later completes successfully; SCRAP/FINISHED_GOODS_REJECT
    // need a manual Resolve action since there's no automatic "dealt with" signal for those.
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private NcrStatus status = NcrStatus.OPEN;

    private String resolvedBy;
    private LocalDateTime resolvedAt;
    private String resolutionRemarks;

    @PrePersist
    protected void onCreate() {
        if (this.reportedAt == null) this.reportedAt = LocalDateTime.now();
    }
}
