package com.aerospace.qa_service.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class EquipmentResponseDTO {
    private Long id;
    private String assetTag;
    private String name;
    private String equipmentType;
    private String manufacturer;
    private String modelNumber;
    private String location;
    private Integer calibrationFrequencyDays;
    private String status;
    private LocalDateTime lastCalibratedAt;
    private LocalDateTime nextCalibrationDueAt;
    private boolean overdue;
    private LocalDateTime createdAt;
}
