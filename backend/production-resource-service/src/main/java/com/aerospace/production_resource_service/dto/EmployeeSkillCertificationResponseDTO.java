package com.aerospace.production_resource_service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class EmployeeSkillCertificationResponseDTO {
    private Long id;
    private Long employeeId;
    private Long skillId;
    private String skillName;
    private Integer level;
    private Integer experienceHours;
    private String certificateNumber;
    private LocalDate issuedDate;
    private LocalDate expiryDate;
    private String evaluatedBy;
    private LocalDate evaluationDate;
    private LocalDate nextEvaluationDate;
    private String status;
    private String remarks;
    private LocalDateTime createdAt;
}
