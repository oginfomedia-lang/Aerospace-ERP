package com.aerospace.finance_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExchangeRateDTO {
    private Long id;

    @NotBlank(message = "From currency is required")
    private String fromCurrency;

    @NotBlank(message = "To currency is required")
    private String toCurrency;

    @NotNull(message = "Rate is required")
    private BigDecimal rate;

    @NotNull(message = "Effective date is required")
    private LocalDate effectiveDate;
}
