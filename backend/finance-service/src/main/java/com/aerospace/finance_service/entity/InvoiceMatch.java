package com.aerospace.finance_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 3-way match record: links a Purchase Order, Goods Receipt Note (GRN), and
 * Supplier Invoice and compares their amounts. Status is computed at match time:
 *   MATCHED      — all three amounts agree within tolerance
 *   PARTIAL      — GRN quantity received is less than PO quantity
 *   DISCREPANCY  — invoice amount exceeds GRN or PO amount; requires resolution
 */
@Entity
@Table(name = "invoice_matches")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InvoiceMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String tenantId;

    @Column(nullable = false, length = 50)
    private String poNumber;       // soft reference — no cross-service FK
    @Column(nullable = false, length = 50)
    private String grnNumber;      // soft reference
    @Column(nullable = false, length = 50)
    private String invoiceNumber;  // soft reference

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal poAmount;
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal grnAmount;
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal invoiceAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchStatus matchStatus;

    @Column(length = 500)
    private String discrepancyNotes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime matchedAt;
    @Column(nullable = false, length = 100)
    private String matchedBy;

    @PrePersist
    void onCreate() { matchedAt = LocalDateTime.now(); }

    public enum MatchStatus { MATCHED, PARTIAL, DISCREPANCY }
}
