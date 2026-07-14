package com.aerospace.procurement_service.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PurchaseOrderRequest {
    @NotNull(message = "Supplier ID is required")
    private Long supplierId;

    private LocalDateTime deliveryDate;
    private String currency;
    private String remarks;

    // Optional per-PO overrides — null means "use the Organization Profile default"
    // at document-render time (see POReceiptDocument.tsx).
    private String shipToName;
    private String shipToAddress;
    private String shipToState;
    private String shipToStateCode;
    private String paymentTerms;
    private String payment;
    private String deliveryTerms;
    private String exciseDuty;
    private String termsAndConditions;

    @NotEmpty(message = "Purchase order must have at least one item")
    private List<POLineItemDTO> items;
}
