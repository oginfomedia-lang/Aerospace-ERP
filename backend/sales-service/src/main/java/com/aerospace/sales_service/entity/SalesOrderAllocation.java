package com.aerospace.sales_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// Records WHICH sales order line an allocation belongs to and WHICH inventory batch it
// came from — inventory-service's own StockBalance has no "for which sales order" field
// (and shouldn't; that's a sales concern, not an inventory one), so this table is the
// bridge between the two. One row per (line, batch) pair — a line can be split across
// several batches if one batch alone isn't enough.
//
// The entity/repository exist now so AtpServiceImpl can already subtract "already
// committed to other orders" from real stock (§18.8) — the actual allocate-on-confirm
// endpoint that CREATES these rows is a later pass (Phase 5), so until then this table
// stays empty and ATP simply sees zero existing commitments.
@Entity
@Table(name = "sales_order_allocations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesOrderAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_line_id", nullable = false)
    @ToString.Exclude
    private SalesOrderLine salesOrderLine;

    private Long partId;
    private String batchNumber;
    private String warehouseLocation;

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AllocationStatus status = AllocationStatus.ACTIVE;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }
}
