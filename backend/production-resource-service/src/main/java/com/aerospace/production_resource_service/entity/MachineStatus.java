package com.aerospace.production_resource_service.entity;

// Physical condition only — NOT occupancy. Whether a machine is "busy right now" is
// a later phase's concern (a MachineAllocation row), deliberately not a status value
// here, since a stored OCCUPIED flag goes stale the moment someone forgets to clear it.
public enum MachineStatus {
    ACTIVE, UNDER_MAINTENANCE, INACTIVE, RETIRED
}
