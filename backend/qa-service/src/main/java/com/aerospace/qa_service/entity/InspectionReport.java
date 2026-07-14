package com.aerospace.qa_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// One row per QA inspection decision made on a batch
// A single batch can have multiple inspection records over its lifetime
@Entity
@Table(name = "inspection_report")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspectionReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Links back to inventory — same partId used in StockBalance
    @Column(nullable = false)
    private Long partId;

    @Column(nullable = false)
    private String batchNumber;

    // APPROVED or REJECTED
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QADecision decision;

    // QA engineer's inspection notes — required for aerospace traceability
    @Column(nullable = false)
    private String remarks;

    // How much of the inspected batch was accepted vs rejected — together they must equal
    // the batch's total pending quantity at inspection time (see InspectionServiceImpl).
    private Integer approvedQty;
    private Integer rejectedQty;

    // The acceptance standard/spec applied for this inspection — distinct from `remarks`
    // (the inspector's free-form notes).
    @Column(length = 2000)
    private String criteria;

    // Optional: certificate or test report reference
    private String certificateNumber;

    // Who made the decision
    private String inspectorName;

    // When the inspection happened
    @Column(nullable = false)
    private LocalDateTime inspectedAt;

    // When this record was published to Kafka (null until published)
    private LocalDateTime publishedAt;

    // Track if Kafka publish succeeded
    @Column(nullable = false)
    @Builder.Default
    private boolean publishedToInventory = false;

    @PrePersist
    protected void onCreate() {
        this.inspectedAt = LocalDateTime.now();
    }
}