package com.aerospace.production_service.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

// Local mirror of order-service's WorkOrderEvents.WorkOrderStatusChangedEvent — consumed
// here only to auto-create a ProductionOrder + BOM the moment a WorkOrder's material is
// fully issued (toStatus == IN_PROGRESS carries the materialLineItems snapshot; every
// other transition leaves it null).
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkOrderStatusChangedEvent {
    private Long workOrderId;
    private String woNumber;
    private String aircraftId;
    private String aircraftType;
    private String taskDescription;
    private String taskCode;
    private String priority;
    private String fromStatus;
    private String toStatus;
    private String changedBy;
    private String remarks;
    private LocalDateTime changedAt;
    private List<MaterialLineSnapshot> materialLineItems;
}
