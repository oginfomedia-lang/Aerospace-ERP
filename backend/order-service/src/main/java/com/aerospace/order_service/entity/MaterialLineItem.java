package com.aerospace.order_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "material_line_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) //load work order only when accessed
    @JoinColumn(name = "work_order_id", nullable = false)
    @ToString.Exclude //prevent circular reference infinite loop
    private WorkOrder workOrder;

    // References inventory-service part ID
    @Column(nullable = false)
    private Long partId;

    private String partName;
    private String partNumber;     // SKU / serial number
    private String batchNumber;    // set after material is issued from inventory

    @Column(nullable = false)
    private Integer quantityRequired;

    private Integer quantityIssued;   // filled when inventory issues stock

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LineItemStatus status;

    private String remarks;

    // Snapshotted from the Part item master (inventory-service) at the time this
    // line was added — same rationale as procurement-service's POLineItem.unitPrice.
    private BigDecimal unitPrice;
    private BigDecimal taxRatePercent;

    @Column(length = 2000)
    private String description;

    public enum LineItemStatus {
        PENDING,    // not yet issued
        PARTIALLY_ISSUED, //some stock issued, remaining pending
        ISSUED,     // stock issued from inventory
        RETURNED,   // unused material returned
        CANCELLED   // line item cancelled
    }
}