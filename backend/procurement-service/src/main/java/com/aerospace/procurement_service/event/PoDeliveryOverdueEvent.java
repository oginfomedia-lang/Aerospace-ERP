package com.aerospace.procurement_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoDeliveryOverdueEvent {
    private Long poId;
    private String poNumber;
    private String supplierName;
    private LocalDateTime deliveryDate;
    private long hoursOverdue;
}