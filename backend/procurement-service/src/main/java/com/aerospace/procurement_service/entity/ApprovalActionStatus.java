package com.aerospace.procurement_service.entity;

// Per-level state within a PO's approval chain (PUR-BR-001).
public enum ApprovalActionStatus {
    WAITING,   // qualifies for approval but an earlier level hasn't cleared yet
    PENDING,   // currently awaiting action at this level
    APPROVED,
    REJECTED
}
