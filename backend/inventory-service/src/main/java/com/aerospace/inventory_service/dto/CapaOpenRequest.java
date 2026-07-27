package com.aerospace.inventory_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CapaOpenRequest {
    @NotNull(message = "CAPA type is required")
    private String capaType; // CORRECTIVE, PREVENTIVE, BOTH
    private String ownerName;
    private LocalDateTime targetCloseDate;
    private String remarks;
    private String createdBy;
}
