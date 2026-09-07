package com.aerospace.production_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

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

    // Tracks which material-issued Kafka deliveries have already been applied to this
    // material line, so a redelivered/replayed copy of the same event (Kafka is
    // at-least-once) is a no-op instead of double-counting quantityConsumed. Populated
    // with a delivery coordinate (topic-partition-offset) by ProductionConsumer.
    @ElementCollection
    @CollectionTable(name = "production_material_applied_refs", joinColumns = @JoinColumn(name = "material_id"))
    @Column(name = "event_ref", length = 255)
    @Builder.Default
    private Set<String> appliedMaterialIssueRefs = new HashSet<>();

    public enum MaterialStatus {
        PENDING,    // not yet drawn from inventory
        CONSUMED,   // used in production
        RETURNED,   // unused material returned to store
        SCRAPPED    // material scrapped / wasted
    }
}