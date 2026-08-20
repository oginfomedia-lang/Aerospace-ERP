package com.aerospace.sales_service.event;

import lombok.*;

// Published on any customer edit, including a status change (ACTIVE/INACTIVE/
// ON_HOLD) — updateCustomer is sales-service's only write path for both, unlike
// procurement's separate updateSupplier/setActive.
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CustomerUpdatedEvent {
    private Long customerId;
    private String customerCode;
    private String name;
    private String status;
    private String updatedBy;
}
