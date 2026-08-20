package com.aerospace.procurement_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Consumed by finance-service's PostingEngine — carries everything needed to raise
// the AP liability journal entry (VENDOR_INVOICE_APPROVED posting rule) with no
// further cross-service lookup.
@Data
@Builder
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
