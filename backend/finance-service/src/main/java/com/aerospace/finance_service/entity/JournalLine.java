package com.aerospace.finance_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "journal_lines")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JournalLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    private JournalEntry journalEntry;

    @Column(nullable = false, length = 30)
    private String accountCode; // soft reference to Account.accountCode

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal debitAmount;  // one of debit/credit is zero
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal creditAmount;
    @Column(nullable = false, length = 3)
    private String currency;

    // optional counterparty tag used by AP/AR aging views — e.g. "SUPPLIER:1042" or
    // "CUSTOMER:207". Kept as a string (not a FK) because it also covers customers,
    // which have no local reference table of their own yet. When `supplier` below is
    // set, this is auto-derived as "SUPPLIER:<id>" so the existing AP aging/ledger/
    // revaluation queries (which group by this string) keep working unmodified.
    @Column(length = 60)
    private String counterpartyRef;

    // Real FK, nullable — only AP lines with a synced supplier have this set (AR/
    // customer lines, and any legacy free-text-only line, leave it null).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", foreignKey = @ForeignKey(name = "fk_journal_line_supplier"))
    private SupplierReference supplier;

    // Mirrors `supplier` above for AR lines — a line is never both AP and AR, so
    // exactly one of supplier/customer is set, or neither.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", foreignKey = @ForeignKey(name = "fk_journal_line_customer"))
    private CustomerReference customer;

    private Long costCenterId;
    private Long profitCenterId;
    @Column(length = 300)
    private String description;
}
