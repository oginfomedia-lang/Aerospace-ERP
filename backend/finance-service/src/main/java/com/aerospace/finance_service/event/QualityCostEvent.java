package com.aerospace.finance_service.event;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class QualityCostEvent {
    private String qualityOrderRef;  // NCR or quality order number
    private BigDecimal totalCost;
    private String currency;
    private Long costCenterId;
}
