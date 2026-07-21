package com.aerospace.production_service.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionOrderNeedingPartResponse {
    private Long productionOrderId;
    private String poNumber;
    private Long materialId;
    private String partName;
    private Integer quantityRequired;
    private Integer quantityConsumed;
}
