package com.aerospace.finance_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Records a payment made to a supplier. On creation, a journal entry is
 * auto-posted (Dr Accounts Payable, Cr Bank/Cash) via the PostingEngine
 * using the SUPPLIER_PAYMENT_MADE trigger. Cancellation reverses the journal.
 */
@Entity
@Table(name = "supplier_payments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupplierPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String tenantId;

    @Column(nullable = false, unique = true, length = 30)
    private String paymentNumber;  // DocumentSequenceService.generate("PAY")

    @Column(nullable = false, length = 50)
    private String invoiceNumber;  // soft reference to the supplier invoice being settled

    // Real FK — the actual input now (see SupplierPaymentCreateDTO.supplierId).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_supplier_payment_supplier"))
    private SupplierReference supplier;

    // @Deprecated: kept for one release as a backward-compatible display field,
    // auto-derived server-side as "SUPPLIER:<supplierId>" — no longer user input.
    @Deprecated
    @Column(length = 60)
    private String supplierRef;

    @Column(nullable = false)
    private LocalDate paymentDate;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.DRAFT;

    @Column(length = 30)
    private String journalEntryRef;  // JournalEntry.journalNumber of the auto-posted entry

    @Column(length = 500)
    private String remarks;

    @Column(nullable = false, length = 100)
    private String createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }

    public enum PaymentMethod { BANK_TRANSFER, CHEQUE, CASH, ONLINE }
    public enum PaymentStatus { DRAFT, POSTED, CANCELLED }
}
