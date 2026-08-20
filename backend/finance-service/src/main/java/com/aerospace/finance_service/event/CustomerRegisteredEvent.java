package com.aerospace.finance_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Local mirror of sales-service's CustomerRegisteredEvent — field names must match
// exactly for Jackson to deserialize it off the wire.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRegisteredEvent {
    private Long customerId;
    private String customerCode;
    private String name;
    private String registeredBy;
}
