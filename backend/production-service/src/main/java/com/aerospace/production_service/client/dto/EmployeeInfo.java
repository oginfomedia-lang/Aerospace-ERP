package com.aerospace.production_service.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

// Local mirror of production-resource-service's EmployeeResponseDTO — only the
// fields production-service actually needs to validate a stage assignment.
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmployeeInfo {
    private Long id;
    private String employeeCode;
    private String name;
    private String status;
    private String occupancy;
    private List<EmployeeSkillInfo> skills;
}
