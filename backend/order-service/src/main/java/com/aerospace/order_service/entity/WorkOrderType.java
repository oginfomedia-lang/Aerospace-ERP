package com.aerospace.order_service.entity;

public enum WorkOrderType {
    SCHEDULED_MAINTENANCE,   // routine A/B/C/D checks
    UNSCHEDULED_REPAIR,      // defect-driven
    MODIFICATION,            // SB / AD compliance
    INSPECTION,              // standalone inspection task
    OVERHAUL                 // major overhaul
}