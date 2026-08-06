package com.aerospace.notification_service.event;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SupplierRegisteredEvent {
    private Long supplierId;
    private String supplierName;
    private String vendorCode;
    private String registeredBy;
}
