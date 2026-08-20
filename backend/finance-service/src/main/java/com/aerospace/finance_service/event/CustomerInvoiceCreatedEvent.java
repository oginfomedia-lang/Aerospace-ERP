package com.aerospace.finance_service.event;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class CustomerInvoiceCreatedEvent {
    private String invoiceNumber;
    private String customerRef;   // e.g. "CUSTOMER:207" — used as counterpartyRef on the GL line
    private BigDecimal totalAmount;
    private String currency;
    private LocalDate invoiceDate;
}
