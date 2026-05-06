package com.aerospace.order_service.entity;

public enum MaterialRequestStatus {
    PENDING,    // raised, waiting for stores to issue
    ISSUED,     // inventory confirmed issue
    PARTIAL,    // only some qty issued (backorder)
    CANCELLED   // line cancelled
}