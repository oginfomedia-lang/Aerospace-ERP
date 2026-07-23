package com.aerospace.procurement_service.event;

import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class QuotationAwardedEvent {
    private String rfqNumber;
    private Long supplierId;
    private String supplierName;
    private String supplierEmail;
    private BigDecimal unitPrice;
}