package com.aerospace.production_resource_service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class EmployeeResponseDTO {
    private Long id;
    private String employeeCode;
    private String name;
    private String designation;
    private String department;
    private String status;
    private List<EmployeeSkillSummaryDTO> skills;
    private Long shiftId;
    private String shiftName;
    private String occupancy;
    private Long currentStageLogId;
    private Long version;
    private LocalDateTime createdAt;
}
