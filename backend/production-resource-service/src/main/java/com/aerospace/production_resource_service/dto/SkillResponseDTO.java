package com.aerospace.production_resource_service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SkillResponseDTO {
    private Long id;
    private String name;
    private String description;
    private boolean certificationRequired;
}
