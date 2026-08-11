package com.aerospace.production_resource_service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ToolingResponseDTO {
    private Long id;
    private String toolCode;
    private String name;
    private String toolingType;
    private String location;
    private String status;
    private String occupancy;
    private Long currentStageLogId;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
