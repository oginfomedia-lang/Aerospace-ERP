package com.aerospace.reporting_service.event;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PurchaseOrderCreatedEvent {
    private Long purchaseOrderId;
    private String poNumber;
    private String supplierName;
    private Double totalAmount;
    private String currency;
    private LocalDateTime expectedDeliveryDate;
}