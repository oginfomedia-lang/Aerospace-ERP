package com.aerospace.reporting_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InventoryValueDTO {
    private String partName;
    private double value;
}
