package com.aerospace.production_resource_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ToolingRequestDTO {
    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Tooling type is required")
    private String toolingType; // CUTTING_TOOL, FIXTURE, JIG, MEASURING_EQUIPMENT, OTHER

    private String location;
    private String status; // ACTIVE, WORN, RETIRED — ignored on create, always forced ACTIVE
}
