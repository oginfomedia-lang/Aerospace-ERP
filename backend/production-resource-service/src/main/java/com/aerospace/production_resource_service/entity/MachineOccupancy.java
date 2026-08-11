package com.aerospace.production_resource_service.entity;

// Whether this machine is busy on a production stage right now. Deliberately
// separate from MachineStatus (physical condition) — see MachineStatus's own
// comment for why occupancy was originally kept out of that enum.
public enum MachineOccupancy {
    FREE, IN_OPERATION
}
