package com.aerospace.finance_service.event;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class InventoryAdjustmentEvent {
    private String adjustmentNumber;
    private BigDecimal adjustmentAmount; // positive = gain, negative = loss
    private String currency;
}
