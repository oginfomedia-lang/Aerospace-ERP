package com.aerospace.production_service.dto;

import lombok.Data;

@Data
public class StartStageRequestDTO {
    private Long machineId;
    private Long employeeId;
    private String operatorName; // optional — server fills it from the employee's name if left blank
}
