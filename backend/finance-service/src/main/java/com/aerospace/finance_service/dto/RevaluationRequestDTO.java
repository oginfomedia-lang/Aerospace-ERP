package com.aerospace.finance_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RevaluationRequestDTO {
    @NotBlank(message = "Control account code is required (e.g. 2000 for AP, 1200 for AR)")
    private String controlAccountCode;
    @NotBlank(message = "Foreign currency code is required (e.g. USD, EUR)")
    private String foreignCurrency;
    @NotNull(message = "asOfDate is required")
    private LocalDate asOfDate;
}
