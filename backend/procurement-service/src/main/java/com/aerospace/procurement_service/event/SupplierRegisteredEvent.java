package com.aerospace.procurement_service.event;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SupplierRegisteredEvent {
    private long supplierId;
    private String supplierName;
    private String vendorCode;
    private String registeredBy;
}
