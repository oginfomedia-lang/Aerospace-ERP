package com.aerospace.sales_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

// A loose (non-FK) reference to inventory-service's Part — cross-service references in
// this codebase are always plain ids + a denormalized display label, never a real join,
// since the two live in separate databases. Same pattern as RoutingOperation.preferredMachineId.
@Entity
@Table(name = "quotation_lines")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotationLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_id", nullable = false)
    @ToString.Exclude
    private Quotation quotation;

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

    // lineTotal = quantity * unitPrice * (1 - discountPercent/100) * (1 + taxPercent/100)
    // — computed server-side in QuotationServiceImpl, never trusted from the client.
    @Column(precision = 15, scale = 2)
    private BigDecimal lineTotal;
}