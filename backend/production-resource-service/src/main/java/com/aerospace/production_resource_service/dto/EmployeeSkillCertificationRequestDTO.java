package com.aerospace.production_resource_service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class EmployeeSkillCertificationRequestDTO {
    @NotNull(message = "Skill is required")
    private Long skillId;

    @NotNull(message = "Level is required")
    @Min(value = 1, message = "Level must be between 1 and 5")
    @Max(value = 5, message = "Level must be between 1 and 5")
    private Integer level;

    private Integer experienceHours;
    private String certificateNumber;
    private LocalDate issuedDate;
    private LocalDate expiryDate;
    private String evaluatedBy;
    private LocalDate evaluationDate;
    private LocalDate nextEvaluationDate;
    private String remarks;
    private String status;
}
