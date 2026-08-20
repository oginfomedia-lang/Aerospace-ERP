package com.aerospace.finance_service.event;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class CustomerInvoiceCreatedEvent {
    private String invoiceNumber;
    private Long customerId;      // real FK — resolved against CustomerReference
    @Deprecated
    private String customerRef;   // superseded by customerId; kept for backward compat
    private BigDecimal totalAmount;
    private String currency;
    private LocalDate invoiceDate;
}
