package com.aerospace.production_resource_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MachineRequestDTO {
    @NotBlank(message = "Machine code is required")
    private String machineCode;

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Machine type is required")
    private String machineType;

    private String manufacturer;
    private String modelNumber;
    private String location;
    private String capacity;

    @NotNull(message = "Maintenance frequency (days) is required")
    @Min(value = 1, message = "Maintenance frequency must be at least 1 day")
    private Integer maintenanceFrequencyDays;

    private Long linkedEquipmentId;
}
