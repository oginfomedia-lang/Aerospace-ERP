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
    // "CUSTOMER:207", loose string, not a FK
    @Column(length = 60)
    private String counterpartyRef;

    private Long costCenterId;
    private Long profitCenterId;
    @Column(length = 300)
    private String description;
}
