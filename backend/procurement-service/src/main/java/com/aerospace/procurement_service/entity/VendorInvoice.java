package com.aerospace.procurement_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Three-way match: a vendor's invoice is matched against the PO's negotiated price and the
// QA-accepted quantity (POLineItem.acceptedQuantity — already synced from inventory-service
// via Kafka, so no cross-service call is needed here). Lives in procurement-service alongside
// PurchaseOrder/Supplier, so the PO reference below is a real JPA foreign key.
@Entity
@Table(name = "vendor_invoices")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorInvoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String invoiceNumber; // system-generated, e.g. INV/2026/00001

    private String supplierInvoiceRef; // the vendor's own invoice number — free text

    @ManyToOne
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    private LocalDateTime invoiceDate;
    private String currency;
    private BigDecimal totalAmount; // as claimed by the vendor

    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;

    @Column(length = 1000)
    private String remarks;

    @OneToMany(mappedBy = "vendorInvoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<VendorInvoiceLineItem> items = new java.util.ArrayList<>();

    private String approvedBy;
    private LocalDateTime approvedAt;
    private LocalDateTime paidAt;

    // Who actually created this invoice record — needed so the approval self-check (an
    // approver can't approve an invoice they themselves entered) has something to compare
    // against. Stamped the same way as Supplier.createdBy: from the security context at
    // persist time.
    private String createdBy;

    @PrePersist
    private void stampCreatedBy() {
        if (this.createdBy == null) {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            this.createdBy = auth != null ? auth.getName() : "SYSTEM";
        }
    }
}
