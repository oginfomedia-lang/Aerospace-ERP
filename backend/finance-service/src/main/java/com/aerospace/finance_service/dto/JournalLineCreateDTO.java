package com.aerospace.finance_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JournalLineCreateDTO {

    @NotBlank(message = "Account code is required")
    private String accountCode;

    private BigDecimal debitAmount;
    private BigDecimal creditAmount;

    @NotBlank(message = "Currency is required")
    private String currency;

    private String counterpartyRef;
    private Long costCenterId;
    private Long profitCenterId;
    private String description;
}
