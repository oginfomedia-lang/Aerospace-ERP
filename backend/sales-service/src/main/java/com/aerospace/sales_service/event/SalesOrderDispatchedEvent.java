package com.aerospace.sales_service.event;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SalesOrderDispatchedEvent {
    private Long salesOrderId;
    private String orderNumber;
    private String dispatchNumber;
    private String trackingNumber;
    private Long customerId;
}
