package com.aerospace.sales_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "return_authorizations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnAuthorization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String rmaNumber; // e.g. RMA-2026-000001

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_id", nullable = false)
    @ToString.Exclude
    private SalesOrder salesOrder;

    private Long partId;
    private String partNumber;

    @Column(nullable = false)
    private Integer quantity;

    private String reasonCode; // e.g. "DAMAGED_IN_TRANSIT", "WRONG_ITEM", "QUALITY_DEFECT"
    private String reasonDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReturnDisposition disposition = ReturnDisposition.PENDING_INSPECTION;

    // Set once the physical goods actually arrive and are received into inventory —
    // null means "not yet received". This is the system-generated batch inventory-
    // service assigned on receipt, remembered here so inspectAndDispose knows exactly
    // which batch to release/reject.
    private String receivedBatchNumber;

    private String inspectedBy;
    private LocalDateTime inspectedAt;
    private String inspectionNotes;

    private String requestedBy;
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }
}
