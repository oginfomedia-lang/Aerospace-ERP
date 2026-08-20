package com.aerospace.finance_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Local mirror of procurement-service's VendorInvoiceApprovedEvent — field names must
// match exactly for Jackson to deserialize it off the wire.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VendorInvoiceApprovedEvent {
    private Long vendorInvoiceId;
    private String invoiceNumber;
    private Long supplierId;
    private BigDecimal totalAmount;
    private String currency;
    private String approvedBy;
    private LocalDateTime approvedAt;
}
