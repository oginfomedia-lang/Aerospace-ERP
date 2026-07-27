package com.aerospace.notification_service.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Published by qa-service's CalibrationDueScheduler when a gauge/tool is due or
 * overdue for calibration. Local mirror — field names must match qa-service's copy.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CalibrationDueEvent {
    private String assetTag;
    private String equipmentName;
    private String equipmentType;
    private String location;
    private LocalDateTime nextCalibrationDueAt;
    private boolean overdue;
    private long daysUntilDue;
}
