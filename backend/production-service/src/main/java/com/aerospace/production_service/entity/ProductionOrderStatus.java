package com.aerospace.production_service.entity;

public enum ProductionOrderStatus {
    PLANNED,             // created, not yet started
    RELEASED,            // released to production floor
    IN_PRODUCTION,       // manufacturing in progress (per-BOM-line routing operations)
    READY_FOR_ASSEMBLY,  // every BOM line has finished its routing — set automatically
    IN_ASSEMBLY,         // the order-level Final Assembly stage is running
    QUALITY_CHECK,       // final assembly done, awaiting QA
    COMPLETED,           // QA passed, order fulfilled
    ON_HOLD,             // paused due to material/QA issue
    CANCELLED            // cancelled before completion
}