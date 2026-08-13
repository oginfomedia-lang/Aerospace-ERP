package com.aerospace.sales_service.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class AtpResultDTO {
    private Long salesOrderLineId;
    private Long partId;
    private String partNumber;
    private Integer requestedQuantity;
    private Integer availableNow;      // on-hand, minus what's already allocated to OTHER sales orders
    private boolean canFulfillNow;     // availableNow >= requestedQuantity
    private LocalDate earliestDeliveryDate; // today if canFulfillNow, else a rough estimate
    private Integer shortageQuantity;  // 0 if canFulfillNow
}
