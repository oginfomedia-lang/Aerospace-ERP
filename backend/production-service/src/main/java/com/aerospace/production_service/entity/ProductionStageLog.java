package com.aerospace.production_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "production_stage_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionStageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_order_id", nullable = false)
    @ToString.Exclude
    private ProductionOrder productionOrder;

    // Null means an ORDER-LEVEL stage (only ever "Final Assembly" — see
    // ProductionOrderServiceImpl.startAssembly). Every routing-expanded, per-BOM-line
    // stage sets this so all N BOM items can progress their own sequence in parallel.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_material_id")
    @ToString.Exclude
    private ProductionMaterial productionMaterial;

    // Position in this BOM line's routing (from RoutingOperation.sequenceNumber).
    // Null for the order-level Final Assembly stage.
    private Integer sequenceNumber;

    // Incremented each time a REWORK disposition sends this same sequence step back
    // for a redo — gating logic always reads the latest attempt for a sequence.
    @Builder.Default
    private Integer attemptNumber = 1;

    // Traceability back to the RoutingOperation template row that generated this stage.
    private Long routingOperationId;

    @Column(nullable = false)
    private String stageName;     // e.g. "Cutting", "Assembly", "Painting", "QA"

    // Copied from the RoutingOperation at routing-expansion time — PRODUCTION stages
    // just report quantity produced; QA_INSPECTION stages make the accept/rework/scrap
    // decision. Null for the order-level Final Assembly stage (productionMaterial null).
    @Enumerated(EnumType.STRING)
    private OperationType operationType;

    private String stageCode;     // internal code e.g. STG-01

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StageStatus status;

    private Integer quantityIn;   // units entering this stage
    private Integer quantityOut;  // units leaving this stage (after scrap)
    private Integer scrapCount;   // rejected at this stage

    private String operatorName;
    private String remarks;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    @Column(nullable = false)
    private LocalDateTime loggedAt;

    @PrePersist
    protected void onCreate() {
        this.loggedAt = LocalDateTime.now();
    }

    public enum StageStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }
}