package com.aerospace.qa_service.event;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

// Published by CalibrationDueScheduler, consumed by notification-service.
// Field names must match notification-service's local mirror copy of this class.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalibrationDueEvent {
    private String assetTag;
    private String equipmentName;
    private String equipmentType;
    private String location;
    private LocalDateTime nextCalibrationDueAt;
    private boolean overdue;
    private long daysUntilDue;
}
