package com.aerospace.procurement_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "purchase_orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String poNumber; // e.g., PO/2026/00001 (ARC-BR-006, system-generated)

    @ManyToOne
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Enumerated(EnumType.STRING)
    private POStatus status;

    private LocalDateTime orderDate;
    private LocalDateTime deliveryDate;

    private BigDecimal totalAmount;
    private String currency; // e.g., "USD", "EUR"

    // orphanRemoval lets edit-in-place replace the line item list wholesale (clear + re-add)
    // and have the old rows actually deleted, not just unlinked
    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<POLineItem> items = new java.util.ArrayList<>();

    private String remarks;

    // Which approval level is currently pending; null when not in an approval chain (PUR-BR-001)
    private Integer currentApprovalLevel;

    // Incremented on every (re)submission — a rejection sends the PO back to Draft and the
    // next submit starts a fresh chain under a new version (PUR-BR-002/PUR-BR-003)
    // ColumnDefault is required (not just @Builder.Default) so Hibernate's ALTER TABLE on the
    // already-populated purchase_orders table can add this NOT NULL column without failing.
    @Builder.Default
    @ColumnDefault("1")
    @Column(nullable = false)
    private int documentVersion = 1;
}