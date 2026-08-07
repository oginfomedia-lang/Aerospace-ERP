package com.aerospace.production_resource_service.event;

import lombok.*;
import java.time.LocalDateTime;

// Published by MaintenanceDueScheduler, consumed by notification-service.
// Field names must match notification-service's local mirror copy of this class.
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MaintenanceDueEvent {
    private String machineCode;
    private String machineName;
    private String machineType;
    private String location;
    private LocalDateTime nextMaintenanceDueAt;
    private boolean overdue;
    private long daysUntilDue;
}
