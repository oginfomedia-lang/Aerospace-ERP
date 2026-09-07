package com.aerospace.procurement_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "purchase_orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String poNumber; // e.g., PO/2026/00001 (ARC-BR-006, system-generated)

    @ManyToOne
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Enumerated(EnumType.STRING)
    private POStatus status;

    private LocalDateTime orderDate;
    private LocalDateTime deliveryDate;

    private BigDecimal totalAmount;
    private String currency; // e.g., "USD", "EUR"

    // orphanRemoval lets edit-in-place replace the line item list wholesale (clear + re-add)
    // and have the old rows actually deleted, not just unlinked
    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<POLineItem> items = new java.util.ArrayList<>();

    private String remarks;

    // Set only when this PO was auto-raised for a Work Order material shortage —
    // lets the UI trace a shortage all the way from the WO to the PO and back.
    private Long sourceWorkOrderId;
    private String sourceWoNumber;

    private Long sourcePurchaseRequisitionId;
    private Long sourceRfqId;
    private Long sourceQuotationId;

    // Optional per-PO overrides for the printed document — null means "use the
    // Organization Profile default" at render time (see POReceiptDocument.tsx).
    private String shipToName;
    private String shipToAddress;
    private String shipToState;
    private String shipToStateCode;
    @Column(length = 1000)
    private String paymentTerms;
    @Column(length = 1000)
    private String payment;
    @Column(length = 1000)
    private String deliveryTerms;
    @Column(length = 1000)
    private String exciseDuty;
    @Column(length = 4000)
    private String termsAndConditions;

    // Which approval level is currently pending; null when not in an approval chain (PUR-BR-001)
    private Integer currentApprovalLevel;

    // Incremented on every (re)submission — a rejection sends the PO back to Draft and the
    // next submit starts a fresh chain under a new version (PUR-BR-002/PUR-BR-003)
    // ColumnDefault is required (not just @Builder.Default) so Hibernate's ALTER TABLE on the
    // already-populated purchase_orders table can add this NOT NULL column without failing.
    @Builder.Default
    @ColumnDefault("1")
    @Column(nullable = false)
    private int documentVersion = 1;

    //set once an overdue-delivery notification has fired for this PO — prevents the
    // daily scheduler from re-notifying every single day for the same overdue PO.
    private LocalDateTime overdueNotifiedAt;

    // Who actually created this PO — needed so the approval self-check (an approver can't
    // approve their own document) has something to compare against. Stamped the same way
    // as Supplier.createdBy: from the security context at persist time, falling back to
    // "SYSTEM" for the Kafka-driven auto-raise flows where there's no authenticated user.
    private String createdBy;

    @PrePersist
    private void stampCreatedBy() {
        if (this.createdBy == null) {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            this.createdBy = auth != null ? auth.getName() : "SYSTEM";
        }
    }
}