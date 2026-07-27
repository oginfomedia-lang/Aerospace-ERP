package com.aerospace.qa_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

// What's submitted when registering a new gauge/tool/instrument
@Data
public class EquipmentRequestDTO {

    @NotBlank(message = "Asset tag is required")
    private String assetTag;

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Equipment type is required")
    private String equipmentType; // GAUGE, TOOL, INSTRUMENT, FIXTURE, TEST_EQUIPMENT

    private String manufacturer;
    private String modelNumber;
    private String location;

    @NotNull(message = "Calibration frequency (days) is required")
    @Min(value = 1, message = "Calibration frequency must be at least 1 day")
    private Integer calibrationFrequencyDays;
}
