package com.aerospace.sales_service.entity;

// DRAFT: being built, freely editable in place.
// PENDING_APPROVAL / APPROVED / REJECTED: driven by common-lib's ApprovalEngine (later).
// CONFIRMED: approved and locked.
// ALLOCATED / IN_FULFILLMENT / DISPATCHED / DELIVERED / CLOSED: fulfillment lifecycle (later phases).
// CANCELLED: terminal, from any pre-DISPATCHED state.
public enum SalesOrderStatus {
    DRAFT, PENDING_APPROVAL, APPROVED, REJECTED, CONFIRMED,
    ALLOCATED, IN_FULFILLMENT, DISPATCHED, DELIVERED, CLOSED, CANCELLED
}
