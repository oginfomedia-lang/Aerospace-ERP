package com.aerospace.procurement_service.entity;

public enum POStatus {
    DRAFT,
    PENDING_APPROVAL, // amount-based approval chain in progress (PUR-BR-001)
    APPROVED,         // cleared the full approval chain — ready to send to vendor
    REJECTED,         // transient: immediately routed back to DRAFT by the service layer (PUR-BR-002)
    SENT_TO_VENDOR,
    PARTIALLY_RECEIVED,
    RECEIVED,
    CLOSED,
    CANCELLED
}
