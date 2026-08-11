package com.aerospace.production_resource_service.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

// Local mirror of qa-service's EquipmentResponseDTO — only the fields needed to
// decide whether a Machine's linked calibration record is currently valid.
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EquipmentInfo {
    private Long id;
    private String assetTag;
    private String status;
    private boolean overdue;
}
