package com.aerospace.production_service.entity;

public enum ProductionOrderStatus {
    PLANNED,        // created, not yet started
    RELEASED,       // released to production floor
    IN_PRODUCTION,  // manufacturing in progress
    QUALITY_CHECK,  // finished manufacturing, awaiting QA
    COMPLETED,      // QA passed, order fulfilled
    ON_HOLD,        // paused due to material/QA issue
    CANCELLED       // cancelled before completion
}