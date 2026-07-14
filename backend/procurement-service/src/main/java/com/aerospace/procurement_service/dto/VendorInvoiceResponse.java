package com.aerospace.procurement_service.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class VendorInvoiceResponse {
    private Long id;
    private String invoiceNumber;
    private String supplierInvoiceRef;

    private Long purchaseOrderId;
    private String poNumber;
    private Long supplierId;
    private String supplierName;

    private LocalDateTime invoiceDate;
    private String currency;
    private BigDecimal totalAmount;
    private String status;
    private String remarks;

    private List<VendorInvoiceLineItemDTO> items;

    private String approvedBy;
    private LocalDateTime approvedAt;
    private LocalDateTime paidAt;
}
