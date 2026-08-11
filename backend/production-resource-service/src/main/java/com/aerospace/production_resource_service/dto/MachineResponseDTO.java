package com.aerospace.production_resource_service.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class MachineResponseDTO {
    private Long id;
    private String machineCode;
    private String name;
    private String machineType;
    private String manufacturer;
    private String modelNumber;
    private String location;
    private String capacity;
    private Integer maintenanceFrequencyDays;
    private String status;
    private Long linkedEquipmentId;
    private LocalDateTime lastMaintenanceAt;
    private LocalDateTime nextMaintenanceDueAt;
    private boolean overdue;
    private String occupancy;
    private Long currentStageLogId;
    private Long version;
    private LocalDateTime createdAt;
}
