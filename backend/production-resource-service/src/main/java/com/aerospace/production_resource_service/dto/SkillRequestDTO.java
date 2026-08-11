package com.aerospace.production_resource_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SkillRequestDTO {
    @NotBlank(message = "Skill name is required")
    private String name;

    private String description;
    private boolean certificationRequired;
}
