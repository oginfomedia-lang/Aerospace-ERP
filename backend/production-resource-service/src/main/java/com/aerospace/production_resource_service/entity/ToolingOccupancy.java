package com.aerospace.production_resource_service.entity;

// Whether this tool is in use on a production stage right now. Separate from
// ToolingStatus, same reasoning as MachineOccupancy vs MachineStatus.
public enum ToolingOccupancy {
    FREE, IN_OPERATION
}
