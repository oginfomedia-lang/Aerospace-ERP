package com.aerospace.finance_service.dto;

import com.aerospace.finance_service.entity.Account.AccountType;
import lombok.*;

import java.math.BigDecimal;

/** One row in the Trial Balance report. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrialBalanceLineDTO {
    private String accountCode;
    private String accountName;
    private AccountType accountType;
    private String financialCategory;
    private BigDecimal totalDebit;   // sum of all posted debit amounts on this account
    private BigDecimal totalCredit;  // sum of all posted credit amounts on this account
    private BigDecimal netBalance;   // totalDebit - totalCredit
}
