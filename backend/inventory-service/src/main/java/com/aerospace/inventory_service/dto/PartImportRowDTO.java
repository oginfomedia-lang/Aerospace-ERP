package com.aerospace.inventory_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartImportRowDTO {
    private String name;
    private String skuCode;
    private String partType;
    private String uom;
    private Integer reorderLevel;
    private String criticality;
}
