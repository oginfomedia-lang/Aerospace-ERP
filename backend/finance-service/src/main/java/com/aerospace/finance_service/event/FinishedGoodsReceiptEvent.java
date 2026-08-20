package com.aerospace.finance_service.event;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class FinishedGoodsReceiptEvent {
    private String receiptNumber;
    private BigDecimal productionCost;  // total cost of manufacturing the batch
    private String currency;
    private Long costCenterId;
}
