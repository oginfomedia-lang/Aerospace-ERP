package com.aerospace.procurement_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// Immutable approval trail per PO per submission (pur_approval_history). Rows are never
// edited after an approve/reject action is recorded — a rejection + resubmission creates
// a brand new set of rows under the next documentVersion (PUR-BR-002/PUR-BR-003).
@Entity
@Table(name = "po_approval_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class POApprovalHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @Column(nullable = false)
    private int approvalLevel;

    @Column(nullable = false)
    private String approverRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalActionStatus status;

    private String approverUsername;
    private LocalDateTime approvedAt;

    @Column(length = 1000)
    private String remarks;

    private String digitalSignatureHash;

    @Column(nullable = false)
    private int documentVersion;
}
