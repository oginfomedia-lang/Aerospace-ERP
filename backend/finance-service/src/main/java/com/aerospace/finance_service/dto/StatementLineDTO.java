package com.aerospace.finance_service.dto;

import lombok.*;

import java.math.BigDecimal;

/** A single line in a P&L or Balance Sheet statement. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StatementLineDTO {
    private String accountCode;
    private String accountName;
    private String category;       // e.g. "Current Asset", "Operating Expense"
    private BigDecimal amount;     // signed — positive = credit-natural accounts have positive balance
}
