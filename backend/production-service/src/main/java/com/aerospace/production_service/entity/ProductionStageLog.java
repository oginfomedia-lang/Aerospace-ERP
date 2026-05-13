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

    @Column(nullable = false)
    private String stageName;     // e.g. "Cutting", "Assembly", "Painting", "QA"

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