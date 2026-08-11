package com.aerospace.production_resource_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EmployeeReservationRequestDTO {
    @NotNull(message = "Employee is required")
    private Long employeeId;

    @NotNull(message = "Reservation start is required")
    private LocalDateTime reservedFrom;

    @NotNull(message = "Reservation end is required")
    private LocalDateTime reservedTo;

    private Long productionOrderId;

    @NotBlank(message = "createdBy is required")
    private String createdBy;

    // Optional — the caller (planner) supplies these directly rather than this
    // service reaching back into production-service to look up a RoutingOperation,
    // which would create a circular cross-service call. Same denormalized-context
    // pattern already used for preferredMachineCode/preferredEmployeeName.
    private Long requiredSkillId;
    private Integer minimumSkillLevel;
}
