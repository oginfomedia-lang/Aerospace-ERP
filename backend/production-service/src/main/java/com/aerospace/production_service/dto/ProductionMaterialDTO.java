package com.aerospace.production_service.dto;

import com.aerospace.production_service.entity.ProductionMaterial.MaterialStatus;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionMaterialDTO {

    private Long id;

    // References inventory-service Part
    private Long partId;
    private String partName;
    private String partNumber;
    private String batchNumber;

    private Integer quantityRequired;
    private Integer quantityConsumed;
    private Integer quantityWasted;

    private MaterialStatus status;
    private String remarks;
    private Boolean readyForAssembly;
}
