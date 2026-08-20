package com.aerospace.finance_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// Local mirror of sales-service's SalesOrderDispatchedEvent — field names must match
// exactly for Jackson to deserialize it off the wire.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesOrderDispatchedEvent {
    private Long salesOrderId;
    private String orderNumber;
    private String dispatchNumber;
    private String trackingNumber;
    private Long customerId;
    private String dispatchedBy;
    private BigDecimal totalAmount;
    private String currency;
}
