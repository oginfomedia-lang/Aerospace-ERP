package com.aerospace.order_service.entity;

public enum WorkOrderStatus {
    DRAFT,          // created but not yet submitted
    OPEN,           // submitted, awaiting material allocation
    IN_PROGRESS,    // material issued, work started
    ON_HOLD,        // blocked — waiting for QA / parts
    COMPLETED,      // all tasks done
    CANCELLED       // cancelled before completion
}