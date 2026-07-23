package com.aerospace.notification_service.event;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class QuotationRejectedEvent {
    private String rfqNumber;
    private Long supplierId;
    private String supplierName;
    private String supplierEmail;
}