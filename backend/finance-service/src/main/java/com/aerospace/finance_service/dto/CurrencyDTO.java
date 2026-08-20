package com.aerospace.finance_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CurrencyDTO {

    @NotBlank(message = "Currency code is required")
    private String currencyCode;

    @NotBlank(message = "Currency name is required")
    private String currencyName;

    private Boolean isBaseCurrency;
    private Boolean activeStatus;
}
