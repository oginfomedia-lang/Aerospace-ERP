package com.aerospace.sales_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales_orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderNumber; // e.g. SO-2026-000001 — stays the same across future revisions

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @ToString.Exclude
    private Customer customer;

    private String orderType; // e.g. "STANDARD", "RUSH", "SAMPLE" — free text

    private LocalDate requestedDeliveryDate;
    private LocalDate promisedDeliveryDate; // set by ATP later — not hand-typed

    private String shippingAddress;
    private String priority; // LOW, MEDIUM, HIGH, URGENT — same convention as ProductionOrder.priority

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SalesOrderStatus status = SalesOrderStatus.DRAFT;

    @Column(precision = 15, scale = 2)
    private BigDecimal totalAmount; // recomputed server-side from lines, same principle as Quotation

    // Which Quotation this came from, if any — null for an order created directly.
    private Long sourceQuotationId;

    // Versioning fields reserved for a later pass (editing a CONFIRMED order will
    // supersede it with a new revision, mirroring RoutingTemplate's history design) —
    // present now so the schema doesn't need another migration later; a fresh order
    // is always revision 1, current, with no predecessor.
    @Builder.Default
    private Integer revisionNumber = 1;
    private Long supersedesId;
    @Builder.Default
    private Boolean isCurrentRevision = true;

    @OneToMany(mappedBy = "salesOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<SalesOrderLine> lines = new ArrayList<>();

    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
