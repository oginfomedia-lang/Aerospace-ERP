package com.aerospace.production_resource_service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SkillUpgradeRequestCreateDTO {
    @NotNull(message = "Skill is required")
    private Long skillId;

    @NotNull(message = "Target level is required")
    @Min(value = 1, message = "Level must be between 1 and 5")
    @Max(value = 5, message = "Level must be between 1 and 5")
    private Integer toLevel;

    private String reason;
    private String requestedBy;
}
