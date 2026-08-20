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

    // Optional real FK — when set, JournalEntryServiceImpl resolves it against
    // SupplierReferenceRepository and auto-derives counterpartyRef if the caller
    // didn't also supply one explicitly. Not @NotNull: most lines (AR/customer lines,
    // non-AP lines) have no supplier at all.
    private Long supplierId;

    // Mirrors supplierId for AR lines — a line must never set both (it's either an
    // AP line, an AR line, or neither, never both).
    private Long customerId;

    private Long costCenterId;
    private Long profitCenterId;
    private String description;
}
