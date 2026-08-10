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
    private List<SkillResponseDTO> skills;
    private LocalDateTime createdAt;
}
