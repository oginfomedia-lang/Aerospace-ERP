package com.aerospace.production_resource_service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MachineReservationResponseDTO {
    private Long id;
    private Long machineId;
    private String machineCode;
    private LocalDateTime reservedFrom;
    private LocalDateTime reservedTo;
    private Long productionOrderId;
    private String createdBy;
    private String status;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
