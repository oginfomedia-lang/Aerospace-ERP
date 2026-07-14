package com.aerospace.inventory_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LocationRequest {
    @NotBlank(message = "Location code is required")
    private String code;

    @NotBlank(message = "Location name is required")
    private String name;

    private String zone;
}
