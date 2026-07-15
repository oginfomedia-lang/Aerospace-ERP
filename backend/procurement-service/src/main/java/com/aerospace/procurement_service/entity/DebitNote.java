package com.aerospace.procurement_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Raised automatically when an NCR is dispositioned RETURN_TO_VENDOR (see
// VendorReturnEvent / DebitNoteServiceImpl.createFromVendorReturn) — the accounts-payable
// record of "the supplier owes us credit for this rejected quantity," so it isn't paid
// for twice. Lives here alongside VendorInvoice/PurchaseOrder, so the PO reference is a
// real JPA foreign key (both entities share this service's database).
@Entity
@Table(name = "debit_notes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebitNote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String debitNoteNumber; // system-generated, e.g. DN/2026/00001

    @ManyToOne
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    private String supplierName; // snapshotted at creation — same convention as VendorInvoice

    private String skuCode;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount; // quantity * unitPrice

    private String ncrNumber;
    private String batchNumber;

    @Enumerated(EnumType.STRING)
    private DebitNoteStatus status;

    @Column(length = 1000)
    private String remarks;

    private LocalDateTime createdAt;
    private LocalDateTime settledAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}