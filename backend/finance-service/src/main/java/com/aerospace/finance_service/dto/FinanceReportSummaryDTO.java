package com.aerospace.finance_service.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FinanceReportSummaryDTO {
    // Journal metrics
    private long totalJournalsSubmitted;
    private long journalsPosted;
    private long journalsRejected;
    private double journalPostingSuccessRatePercent;

    // Period metrics
    private long openPeriodCount;
    private long closedPeriodCount;
    private long lockedPeriodCount;

    // AP / AR balances (base currency)
    private BigDecimal outstandingAP;
    private BigDecimal outstandingAR;

    // P&L summary (base currency)
    private BigDecimal totalRevenuePosted;
    private BigDecimal totalExpensePosted;
    private BigDecimal grossMarginApprox;   // revenue - expense

    // Operational metrics (base currency)
    private BigDecimal inventoryValue;          // sum of ASSET accounts with category "Inventory"
    private BigDecimal manufacturingCostMTD;    // EXPENSE lines for manufacturing cost centers, current month
    private BigDecimal cashPositionApprox;      // sum of ASSET accounts with category "Cash and Bank"
    private BigDecimal operatingExpensesMTD;    // all EXPENSE lines for current month

    // Reporting currency (optional — populated only when ?reportingCurrency= is provided)
    private String reportingCurrency;
    private BigDecimal totalRevenueInReportingCurrency;
    private BigDecimal totalExpenseInReportingCurrency;
    private BigDecimal grossMarginInReportingCurrency;
}
