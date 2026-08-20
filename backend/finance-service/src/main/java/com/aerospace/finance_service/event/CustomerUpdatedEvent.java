package com.aerospace.finance_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Local mirror of sales-service's CustomerUpdatedEvent.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerUpdatedEvent {
    private Long customerId;
    private String customerCode;
    private String name;
    private String status;
    private String updatedBy;
}
