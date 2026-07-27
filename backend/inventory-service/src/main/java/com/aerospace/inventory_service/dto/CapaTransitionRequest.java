package com.aerospace.inventory_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CapaTransitionRequest {
    private String rootCause;
    private String correctiveAction;
    private String preventiveAction;
    private String verificationRemarks;

    @NotBlank(message = "Remarks are required for traceability")
    private String remarks;

    @NotBlank(message = "Actor (your name) is required")
    private String actor;
}
