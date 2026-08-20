package com.aerospace.sales_service.event;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CustomerRegisteredEvent {
    private Long customerId;
    private String customerCode;
    private String name;
    // Customer has no actor-tracking columns today (unlike procurement's Supplier),
    // so this is a fixed value rather than a real audit stamp.
    private String registeredBy;
}
