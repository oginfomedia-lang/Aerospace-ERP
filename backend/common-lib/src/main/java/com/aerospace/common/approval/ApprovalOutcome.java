package com.aerospace.common.approval;

// What the calling module needs to know after an approve/reject action.
public record ApprovalOutcome(Status status, Integer nextLevel, String nextApproverRole) {
    public enum Status { FULLY_APPROVED, PENDING_NEXT_LEVEL, REJECTED }
}