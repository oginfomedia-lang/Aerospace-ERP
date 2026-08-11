package com.aerospace.production_service.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

// Local mirror of production-resource-service's ToolingResponseDTO — only the
// fields production-service actually needs to validate a stage assignment.
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ToolingInfo {
    private Long id;
    private String toolCode;
    private String status;
    private String occupancy;
}
