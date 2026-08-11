package com.aerospace.production_resource_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class EmployeeLeaveRequestDTO {
    @NotNull(message = "Leave type is required")
    private String type; // SICK, VACATION, OTHER

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private String reason;
}
