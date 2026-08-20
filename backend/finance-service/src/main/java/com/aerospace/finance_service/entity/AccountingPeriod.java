package com.aerospace.finance_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounting_periods")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AccountingPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Multi-tenant isolation: every record belongs to exactly one tenant.
    // Stamped by service layer from TenantContext on creation; never changes after that.
    @Column(length = 50)
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fiscal_year_id", nullable = false)
    private FiscalYear fiscalYear;

    @Column(nullable = false)
    private Integer periodNumber; // 1-12

    @Column(nullable = false, length = 20)
    private String periodLabel; // e.g. "2026-08"

    @Column(nullable = false)
    private LocalDate startDate;
    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PeriodStatus status = PeriodStatus.OPEN;

    private LocalDateTime closedAt;
    private String closedBy;
    private LocalDateTime reopenedAt;
    private String reopenedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }
}
