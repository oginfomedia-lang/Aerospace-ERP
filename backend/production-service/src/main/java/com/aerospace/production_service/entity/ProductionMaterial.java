package com.aerospace.production_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "production_materials")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_order_id", nullable = false)
    @ToString.Exclude
    private ProductionOrder productionOrder;

    // References inventory-service part
    private Long partId;
    private String partName;
    private String partNumber;
    private String batchNumber;

    @Column(nullable = false)
    private Integer quantityRequired;

    private Integer quantityConsumed;  // actual usage — may differ from required
    private Integer quantityWasted;    // scrap / wastage

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaterialStatus status;

    private String remarks;

    // Set true once this BOM line's LAST routing stage (latest attempt) completes with
    // quantityOut > 0 — kept separate from MaterialStatus so it doesn't overload the
    // "has it been issued/consumed" semantics that enum already carries. Drives the
    // final-assembly gate (see ProductionOrderServiceImpl.checkFinalAssemblyGate).
    @Builder.Default
    private Boolean readyForAssembly = false;

    public enum MaterialStatus {
        PENDING,    // not yet drawn from inventory
        CONSUMED,   // used in production
        RETURNED,   // unused material returned to store
        SCRAPPED    // material scrapped / wasted
    }
}