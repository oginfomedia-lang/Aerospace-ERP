package com.aerospace.reporting_service.event;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WorkOrderStatusChangedEvent {
    private Long workOrderId;
    private String woNumber;
    private String aircraftId;
    private String taskDescription;
    private String priority;
    private String fromStatus;
    private String toStatus;
    private String changedBy;
    private String remarks;
    private LocalDateTime changedAt;
}