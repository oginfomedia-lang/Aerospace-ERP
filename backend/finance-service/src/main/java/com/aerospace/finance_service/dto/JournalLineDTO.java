package com.aerospace.finance_service.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JournalLineDTO {
    private Long id;
    private String accountCode;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    private String currency;
    private String counterpartyRef;
    private Long costCenterId;
    private Long profitCenterId;
    private String description;
}
