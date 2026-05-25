package com.aerospace.reporting_service.event;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WorkOrderCreatedEvent {
    private Long workOrderId;
    private String woNumber;
    private String aircraftId;
    private String taskDescription;
    private String priority;
    private String status;
    private String createdBy;
    private String location;
    private LocalDateTime createdAt;
}