package com.aerospace.production_resource_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MaintenanceRequestDTO {
    @NotNull(message = "Maintenance type is required")
    private String maintenanceType; // PREVENTIVE, CORRECTIVE, PREDICTIVE

    private LocalDateTime performedAt; // defaults to now() if omitted
    private String performedBy;
    private BigDecimal cost;
    private Integer downtimeMinutes;

    @NotNull(message = "Result is required")
    private String result; // COMPLETED, PARTIAL, FAILED

    @NotBlank(message = "Remarks are mandatory for traceability")
    private String remarks;
}
