package com.aerospace.sales_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dispatch_lines")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispatchLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispatch_id", nullable = false)
    @ToString.Exclude
    private Dispatch dispatch;

    // Loose (non-FK) reference to the SalesOrderLine this dispatch line fulfills — used
    // at confirm time to look up which SalesOrderAllocation batches to actually consume,
    // and to bump that line's quantityShipped.
    private Long salesOrderLineId;

    private Long partId;
    private String partNumber;

    @Column(nullable = false)
    private Integer quantity;
}
