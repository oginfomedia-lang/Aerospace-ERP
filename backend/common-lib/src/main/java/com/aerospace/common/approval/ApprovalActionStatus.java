package com.aerospace.common.approval;

// Per-level state within any document's approval chain.
public enum ApprovalActionStatus {
    WAITING,   // qualifies for approval but an earlier level hasn't cleared yet
    PENDING,   // currently awaiting action at this level
    APPROVED,
    REJECTED
}