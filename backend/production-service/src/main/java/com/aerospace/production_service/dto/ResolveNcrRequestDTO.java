package com.aerospace.production_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResolveNcrRequestDTO {
    @NotBlank(message = "Resolved by is required")
    private String resolvedBy;

    private String resolutionRemarks;
}
