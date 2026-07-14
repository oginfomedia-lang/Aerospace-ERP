package com.aerospace.procurement_service.entity;

public enum InvoiceStatus {
    PENDING_MATCH,  // transient — set then immediately replaced by the match result
    MATCHED,        // every line within quantity/price tolerance of the PO + accepted qty
    VARIANCE_HOLD,  // at least one line breached tolerance — needs an override to approve
    APPROVED,
    REJECTED,
    PAID
}
