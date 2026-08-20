package com.aerospace.finance_service.dto;

import com.aerospace.finance_service.entity.TaxRate.TaxDirection;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TaxCalculationResult {
    private BigDecimal taxAmount;
    private String taxAccountCode;
    private TaxDirection direction;
    private boolean reverseCharge;
    private String reverseChargeCounterAccountCode;
}
