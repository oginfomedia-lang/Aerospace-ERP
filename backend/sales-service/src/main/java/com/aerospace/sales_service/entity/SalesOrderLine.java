package com.aerospace.sales_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "sales_order_lines")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesOrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_id", nullable = false)
    @ToString.Exclude
    private SalesOrder salesOrder;

    private Long partId;
    private String partNumber;
    private String partName;

    @Column(nullable = false)
    private Integer quantity;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal unitPrice;
    @Column(precision = 5, scale = 2)
    private BigDecimal discountPercent;
    @Column(precision = 5, scale = 2)
    private BigDecimal taxPercent;
    @Column(precision = 15, scale = 2)
    private BigDecimal lineTotal;

    // Fulfillment progress per line — filled in by later phases (Allocation, Dispatch).
    // A fresh order-from-quotation always starts both at 0.
    @Builder.Default
    private Integer quantityAllocated = 0;
    @Builder.Default
    private Integer quantityShipped = 0;
}
