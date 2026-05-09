package com.aerospace.notification_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderCompletedEvent {
    private Long workOrderId;
    private String woNumber;
    private String aircraftId;
    private String taskDescription;
    private String completedBy;
    private String closureNotes;
    private LocalDateTime completedAt;
}