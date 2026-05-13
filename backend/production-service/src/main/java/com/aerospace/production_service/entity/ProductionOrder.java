package com.aerospace.production_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "production_orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // e.g. PO-2024-00089 — auto-generated
    @Column(nullable = false, unique = true)
    private String poNumber;

    // Link to work-order-service (optional — PO can exist without a WO)
    private String woNumber;
    private Long workOrderId;

    // What is being produced
    @Column(nullable = false)
    private String productName;

    private String productCode;       // internal part/product code
    private String aircraftType;      // target aircraft type

    @Column(nullable = false)
    private Integer plannedQuantity;

    private Integer producedQuantity; // updated as production progresses
    private Integer rejectedQuantity; // units rejected by QA

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductionOrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductionPriority priority;

    // Scheduling
    private LocalDateTime plannedStartDate;
    private LocalDateTime plannedEndDate;
    private LocalDateTime actualStartDate;
    private LocalDateTime actualEndDate;

    // Assignment
    private String productionLine;    // e.g. "Line A", "Bay 3"
    private String assignedTeam;
    private String supervisorName;
    private String createdBy;

    // Quality
    private String qualityStandard;   // e.g. AS9100, ISO9001
    private String qualityNotes;
    private Boolean qualityPassed;

    // Traceability
    private String batchNumber;
    private String remarks;
    private String closureNotes;

    @Column(nullable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Materials consumed in this production run
    @OneToMany(mappedBy = "productionOrder", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<ProductionMaterial> materials = new ArrayList<>();

    // Stage-by-stage progress log
    @OneToMany(mappedBy = "productionOrder", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductionStageLog> stageLogs = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null)           this.status = ProductionOrderStatus.PLANNED;
        if (this.producedQuantity == null) this.producedQuantity = 0;
        if (this.rejectedQuantity == null) this.rejectedQuantity = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}