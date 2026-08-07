package com.aerospace.notification_service.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MaintenanceDueEvent {
    private String machineCode;
    private String machineName;
    private String machineType;
    private String location;
    private LocalDateTime nextMaintenanceDueAt;
    private boolean overdue;
    private long daysUntilDue;
}
