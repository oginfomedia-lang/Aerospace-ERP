package com.aerospace.inventory_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialShortageEvent {
    private Long partId;
    private String skuCode;
    private String partName;
    private Integer requiredQty;
    private Integer availableQty;
    private Integer shortfallQty;
    private Long preferredSupplierId;
    private Long workOrderId;
    private String woNumber;
}
