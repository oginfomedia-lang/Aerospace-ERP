package com.aerospace.finance_service.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * GST/VAT Return Report: summarises input and output tax for a date range.
 * netTaxPayable = totalOutputTax - totalInputTax.
 * A positive value means the company owes tax to the government.
 * A negative value means the company has excess input tax (refund claimable).
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TaxReturnReportDTO {
    private LocalDate fromDate;
    private LocalDate toDate;
    private BigDecimal totalInputTax;
    private BigDecimal totalOutputTax;
    private BigDecimal netTaxPayable;       // outputTax - inputTax
    private Map<String, BigDecimal> inputTaxByRuleCode;   // e.g. GST_5_INPUT -> 1000
    private Map<String, BigDecimal> outputTaxByRuleCode;  // e.g. GST_18_OUTPUT -> 5000
}
