package com.aerospace.finance_service.dto;

import com.aerospace.finance_service.entity.TaxRate.TaxDirection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TaxRateDTO {
    private Long id;

    @NotBlank(message = "Tax rule code is required")
    private String taxRuleCode;

    private Long taxCategoryId;
    private Long taxJurisdictionId;

    @NotNull(message = "Rate percent is required")
    private BigDecimal ratePercent;

    @NotNull(message = "Direction is required")
    private TaxDirection direction;

    private Boolean reverseCharge;

    @NotBlank(message = "Tax account code is required")
    private String taxAccountCode;

    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Boolean activeStatus;
}
