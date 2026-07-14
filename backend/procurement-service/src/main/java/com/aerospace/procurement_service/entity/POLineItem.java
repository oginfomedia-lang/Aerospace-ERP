package com.aerospace.procurement_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "po_line_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class POLineItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @Column(nullable = false)
    private String skuCode; // References Part SKU from Inventory Service

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private BigDecimal unitPrice;

    private BigDecimal totalPrice;

    private String uom; // Unit of Measure

    // Snapshotted from the Part item master at line-add time (frontend responsibility,
    // same pattern as MaterialLineItem.taxRatePercent on the Work Order side) — a
    // product-classification fact, not a per-PO negotiated value like unitPrice.
    private String hsnCode;
    private BigDecimal taxRatePercent;

    // QA outcome, updated asynchronously from inventory-service's QAStatusChangedEvent
    // once the received goods for this SKU are inspected (see InventoryConsumer).
    @Builder.Default
    private Integer acceptedQuantity = 0;
    @Builder.Default
    private Integer rejectedQuantity = 0;
    private String ncrNumber; // last NCR raised against this line, if any

    @PrePersist
    @PreUpdate
    public void calculateTotal() {
        if (unitPrice != null && quantity != null) {
            this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }
}
