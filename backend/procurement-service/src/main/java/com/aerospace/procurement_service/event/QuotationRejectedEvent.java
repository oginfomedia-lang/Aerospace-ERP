package com.aerospace.procurement_service.event;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class QuotationRejectedEvent {
    private String rfqNumber;
    private Long supplierId;
    private String supplierName;
    private String supplierEmail;
}