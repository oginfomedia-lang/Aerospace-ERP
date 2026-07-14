package com.aerospace.procurement_service.dto;

import lombok.Data;

@Data
public class InvoiceActionRequest {
    // Mandatory when approving a VARIANCE_HOLD invoice (the override reason) or rejecting;
    // optional when approving a clean MATCHED invoice.
    private String remarks;
}
