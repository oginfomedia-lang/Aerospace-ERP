package com.aerospace.inventory_service.entity;

public enum MaterialReceiptStatus {
    RECEIVED_AT_GATE, // goods logged at the gate/dock — no stock/GRN effect yet
    GRN_FORMED        // GRN generated from this receipt — stock posted to PENDING_QA/APPROVED
}
