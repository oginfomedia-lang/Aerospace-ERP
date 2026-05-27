package com.aerospace.reporting_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductionYieldDTO {
    private String poNumber;
    private int planned;
    private int produced;
    private int rejected;
}
