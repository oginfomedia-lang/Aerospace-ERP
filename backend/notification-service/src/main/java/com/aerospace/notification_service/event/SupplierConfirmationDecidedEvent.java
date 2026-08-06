package com.aerospace.notification_service.event;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SupplierConfirmationDecidedEvent {
    private Long supplierId;
    private String supplierName;
    private String status;       // CONFIRMED or REJECTED
    private String decidedBy;
    private String remarks;
    private String registeredBy; // who originally added the supplier
}
