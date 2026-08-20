package com.aerospace.sales_service.event;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SalesOrderDispatchedEvent {
    private Long salesOrderId;
    private String orderNumber;
    private String dispatchNumber;
    private String trackingNumber;
    private Long customerId;
    private String dispatchedBy;
    private BigDecimal totalAmount; // consumed by finance-service's PostingEngine (SALES_DISPATCH_CONFIRMED)
    private String currency;
}
