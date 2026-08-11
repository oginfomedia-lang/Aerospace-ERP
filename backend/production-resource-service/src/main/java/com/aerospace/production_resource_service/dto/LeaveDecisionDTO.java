package com.aerospace.production_resource_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LeaveDecisionDTO {
    @NotBlank(message = "decidedBy is required")
    private String decidedBy;
}
