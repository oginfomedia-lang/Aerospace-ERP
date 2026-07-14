package com.aerospace.procurement_service.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class VendorInvoiceRequest {
    @NotNull(message = "Purchase Order is required")
    private Long purchaseOrderId;

    private String supplierInvoiceRef;
    private LocalDateTime invoiceDate;
    private String currency;
    private String remarks;

    @NotEmpty(message = "Vendor invoice must have at least one item")
    private List<VendorInvoiceLineItemDTO> items;
}
