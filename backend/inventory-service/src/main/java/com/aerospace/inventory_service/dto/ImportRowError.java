package com.aerospace.inventory_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportRowError {
    private int rowNumber;
    private String skuCode;
    private String error;
}
