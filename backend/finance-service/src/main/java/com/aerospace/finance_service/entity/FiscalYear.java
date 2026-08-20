package com.aerospace.finance_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fiscal_years")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FiscalYear {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Multi-tenant isolation: every record belongs to exactly one tenant.
    // Stamped by service layer from TenantContext on creation; never changes after that.
    @Column(length = 50)
    private String tenantId;

    @Column(nullable = false, unique = true, length = 20)
    private String fiscalYearCode; // e.g. "FY2026"

    @Column(nullable = false)
    private LocalDate startDate;
    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PeriodStatus status = PeriodStatus.OPEN;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }
}
