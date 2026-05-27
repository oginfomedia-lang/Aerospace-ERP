package com.aerospace.reporting_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WorkOrderCycleDTO {
    private String woNumber;
    private int daysToComplete;
}
