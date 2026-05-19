package com.aerospace.procurement_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

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
    private Double unitPrice;

    private Double totalPrice;

    private String uom; // Unit of Measure

    @PrePersist
    @PreUpdate
    public void calculateTotal() {
        if (unitPrice != null && quantity != null) {
            this.totalPrice = unitPrice * quantity;
        }
    }
}
