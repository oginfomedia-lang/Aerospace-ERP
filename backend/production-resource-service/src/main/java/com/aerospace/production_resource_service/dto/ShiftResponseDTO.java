package com.aerospace.production_resource_service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalTime;

@Data
@Builder
public class ShiftResponseDTO {
    private Long id;
    private String name;
    private LocalTime startTime;
    private LocalTime endTime;
    private String daysOfWeek;
}
