package com.aerospace.finance_service.event;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class MaintenanceCostEvent {
    private String maintenanceOrderRef;
    private BigDecimal totalCost;
    private String currency;
    private Long costCenterId;
}
