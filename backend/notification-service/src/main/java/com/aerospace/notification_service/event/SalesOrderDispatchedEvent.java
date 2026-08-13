package com.aerospace.notification_service.event;

import lombok.*;

// Local mirror of sales-service's SalesOrderDispatchedEvent.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesOrderDispatchedEvent {
    private Long salesOrderId;
    private String orderNumber;
    private String dispatchNumber;
    private String trackingNumber;
    private Long customerId;
    private String dispatchedBy;
}
