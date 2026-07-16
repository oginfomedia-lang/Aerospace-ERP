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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_material_id", nullable = false)
    @ToString.Exclude
    private ProductionMaterial productionMaterial;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_log_id", nullable = false)
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

    @PrePersist
    protected void onCreate() {
        if (this.reportedAt == null) this.reportedAt = LocalDateTime.now();
    }
}
