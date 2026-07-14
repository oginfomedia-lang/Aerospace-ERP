package com.aerospace.procurement_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "vendor_invoice_line_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorInvoiceLineItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "vendor_invoice_id", nullable = false)
    private VendorInvoice vendorInvoice;

    @Column(nullable = false)
    private String skuCode;

    @Column(nullable = false)
    private Integer invoicedQuantity;

    @Column(nullable = false)
    private BigDecimal invoicedUnitPrice;

    private BigDecimal invoicedTotal;

    // Snapshotted from the matched PO line at match time — the invoice is compared against
    // these, not re-read live, so the audit trail reflects what was actually matched then.
    private Integer matchedPoQuantity;   // the PO line's acceptedQuantity at match time
    private BigDecimal matchedPoUnitPrice;

    private Integer quantityVariance;        // invoicedQuantity - matchedPoQuantity
    private BigDecimal priceVariancePercent; // (invoicedUnitPrice - matchedPoUnitPrice) / matchedPoUnitPrice * 100

    @Enumerated(EnumType.STRING)
    private LineMatchStatus lineStatus;

    public enum LineMatchStatus { MATCHED, VARIANCE, UNMATCHED_SKU }

    @PrePersist
    @PreUpdate
    public void calculateTotal() {
        if (invoicedUnitPrice != null && invoicedQuantity != null) {
            this.invoicedTotal = invoicedUnitPrice.multiply(BigDecimal.valueOf(invoicedQuantity));
        }
    }
}
