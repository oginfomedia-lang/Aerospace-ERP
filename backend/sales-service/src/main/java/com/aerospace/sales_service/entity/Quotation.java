package com.aerospace.sales_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quotations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Quotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String quotationNumber; // e.g. QUOTE-2026-000001

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @ToString.Exclude
    private Customer customer;

    private LocalDate validFrom;
    private LocalDate validUntil;

    private String currency;
    private String paymentTerms;
    private String deliveryTerms;

    @Column(precision = 15, scale = 2)
    private BigDecimal totalAmount; // sum of line items — recomputed server-side, never trusted from the client

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private QuotationStatus status = QuotationStatus.DRAFT;

    // Set once this quotation is accepted and turned into a real SalesOrder — a loose
    // reference within THIS SAME service/database (unlike a cross-service reference,
    // this one is safe to make a real @OneToOne if you prefer; kept as a plain Long
    // here purely for symmetry with how every other "was this converted" flag in this
    // codebase is modeled).
    private Long convertedToSalesOrderId;

    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<QuotationLine> lines = new ArrayList<>();

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