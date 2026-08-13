package com.aerospace.sales_service.entity;

// PENDING doubles as "pick list created, awaiting pack" — a real deployment might split
// a separate PICKED status+timestamp out of this; kept as one status for simplicity here.
public enum DispatchStatus {
    PENDING, PACKED, DISPATCHED
}
