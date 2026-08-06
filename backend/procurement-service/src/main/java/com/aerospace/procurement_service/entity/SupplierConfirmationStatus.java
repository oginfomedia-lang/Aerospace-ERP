package com.aerospace.procurement_service.entity;

public enum SupplierConfirmationStatus {
    PENDING,    // just added, waiting on QA/authority sign-off
    CONFIRMED,  // cleared — eligible for portal invite
    REJECTED    // declined — editing the record puts it back to PENDING
}