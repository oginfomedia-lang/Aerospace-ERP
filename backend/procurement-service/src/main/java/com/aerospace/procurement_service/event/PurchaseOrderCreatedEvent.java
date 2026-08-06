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
public class PurchaseOrderCreatedEvent {
    private Long purchaseOrderId;
    private String poNumber;
    private String supplierName;
    private String supplierEmail;
    private Double totalAmount;
    private String currency;
    private LocalDateTime expectedDeliveryDate;
}
