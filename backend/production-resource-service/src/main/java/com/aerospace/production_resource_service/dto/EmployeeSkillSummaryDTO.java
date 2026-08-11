package com.aerospace.production_resource_service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

// The CURRENT certified state of one skill for an employee — derived from the
// latest non-superseded EmployeeSkillCertification row, not stored directly.
@Data
@Builder
public class EmployeeSkillSummaryDTO {
    private Long skillId;
    private String skillName;
    private Integer level;
    private LocalDate expiryDate;
    private String status;
}
