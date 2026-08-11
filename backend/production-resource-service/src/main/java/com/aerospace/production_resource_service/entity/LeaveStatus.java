package com.aerospace.production_resource_service.entity;

// A leave request's approval state — unlike EmployeeSkillCertification's append-only
// history, a leave request IS one row that moves through a small state machine
// (PENDING -> APPROVED/REJECTED), same as ShopFloorNcr-style single-row status fields
// elsewhere in this app. Past requests are never deleted, just left in their final state.
public enum LeaveStatus {
    PENDING, APPROVED, REJECTED
}
