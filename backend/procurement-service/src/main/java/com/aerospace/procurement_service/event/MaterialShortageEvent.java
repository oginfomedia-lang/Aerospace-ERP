package com.aerospace.procurement_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Local mirror of inventory-service's MaterialShortageEvent — cross-service Kafka
// payloads in this repo are matched by JSON field name (see StockLowEvent), not a
// shared class, since each service deserializes with useTypeHeaders(false).
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
