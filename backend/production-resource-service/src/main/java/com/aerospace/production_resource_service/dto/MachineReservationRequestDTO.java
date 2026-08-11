package com.aerospace.production_resource_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MachineReservationRequestDTO {
    @NotNull(message = "Machine is required")
    private Long machineId;

    @NotNull(message = "Reservation start is required")
    private LocalDateTime reservedFrom;

    @NotNull(message = "Reservation end is required")
    private LocalDateTime reservedTo;

    private Long productionOrderId;

    @NotBlank(message = "createdBy is required")
    private String createdBy;
}
