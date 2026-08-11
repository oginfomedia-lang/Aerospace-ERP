package com.aerospace.production_resource_service.entity;

// CANCELLED rows are kept (never deleted) for planning history — DELETE on the
// reservation APIs means "cancel," not a hard row delete, same traceability-first
// reasoning already applied to ProductionOrder (refuses hard delete once NCRs exist).
public enum ReservationStatus {
    ACTIVE, CANCELLED
}
