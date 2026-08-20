package com.aerospace.finance_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "tax_rates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TaxRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String taxRuleCode; // e.g. "GST_18_OUTPUT" — referenced by PostingRule.taxRuleCode

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "tax_category_id")
    private TaxCategory taxCategory;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "tax_jurisdiction_id")
    private TaxJurisdiction taxJurisdiction;

    @Column(nullable = false, precision = 6, scale = 3)
    private BigDecimal ratePercent; // e.g. 18.000

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaxDirection direction; // INPUT, OUTPUT

    @Column(nullable = false)
    @Builder.Default
    private Boolean reverseCharge = false;

    @Column(nullable = false, length = 30)
    private String taxAccountCode; // GL account this tax posts to

    private LocalDate effectiveFrom;
    private LocalDate effectiveTo; // null = still current

    @Builder.Default
    private Boolean activeStatus = true;

    public enum TaxDirection { INPUT, OUTPUT }
}
