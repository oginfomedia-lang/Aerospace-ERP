package com.aerospace.procurement_service.entity;

import jakarta.persistence.*;
import lombok.*;
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
    private String poNumber; // e.g., PO-2026-001

    @ManyToOne
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Enumerated(EnumType.STRING)
    private POStatus status;

    private LocalDateTime orderDate;
    private LocalDateTime deliveryDate;
    
    private Double totalAmount;
    private String currency; // e.g., "USD", "EUR"

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL)
    private List<POLineItem> items;

    private String remarks;
}