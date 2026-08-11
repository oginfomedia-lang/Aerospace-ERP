package com.aerospace.production_service.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

// Local mirror of production-resource-service's EmployeeSkillSummaryDTO — the
// employee's CURRENT certified level/status for one skill.
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmployeeSkillInfo {
    private Long skillId;
    private String skillName;
    private Integer level;
    private String expiryDate;
    private String status;
}
