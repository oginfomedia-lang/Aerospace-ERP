package com.aerospace.order_service.entity;

public enum WorkOrderStatus {
    DRAFT,          // created but not yet submitted
    OPEN,           // submitted, awaiting material & assignment
    IN_PROGRESS,    // technician has started work
    ON_HOLD,        // paused — waiting for parts, approvals, etc.
    COMPLETED,      // all tasks done, signed off
    CANCELLED       // abandoned
}