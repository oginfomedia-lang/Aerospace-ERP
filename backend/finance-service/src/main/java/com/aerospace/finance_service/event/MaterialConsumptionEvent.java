package com.aerospace.finance_service.event;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class MaterialConsumptionEvent {
    private String consumptionRef;   // e.g. work order number
    private BigDecimal totalCost;
    private String currency;
    private Long costCenterId;       // manufacturing cost center
}
