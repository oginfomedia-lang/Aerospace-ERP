package com.aerospace.finance_service.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LedgerViewDTO {
    private String counterpartyRef;
    private BigDecimal openBalance;
    private int transactionCount;
}
