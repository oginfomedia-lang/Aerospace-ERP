package com.aerospace.reporting_service.event;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WorkOrderCompletedEvent {
    private Long workOrderId;
    private String woNumber;
    private String aircraftId;
    private String taskDescription;
    private String completedBy;
    private String closureNotes;
    private LocalDateTime completedAt;
}