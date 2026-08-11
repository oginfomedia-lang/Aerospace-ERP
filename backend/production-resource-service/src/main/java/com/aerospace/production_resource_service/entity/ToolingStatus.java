package com.aerospace.production_resource_service.entity;

// Physical condition only — NOT occupancy, same separation MachineStatus already
// documents. RETIRED is terminal: once a tool is retired it cannot be reactivated
// (see ToolingServiceImpl.updateTooling's transition check) — a worn/retired tool
// coming back to ACTIVE without a real inspection would be a quality risk.
public enum ToolingStatus {
    ACTIVE, WORN, RETIRED
}
