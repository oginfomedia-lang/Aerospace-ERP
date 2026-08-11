package com.aerospace.production_resource_service.entity;

// Whether this employee is busy on a production stage right now. Deliberately
// separate from EmployeeStatus (employment condition) — same reasoning as
// MachineOccupancy vs MachineStatus.
public enum EmployeeOccupancy {
    FREE, IN_OPERATION
}
