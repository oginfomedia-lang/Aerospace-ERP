package com.aerospace.procurement_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Local mirror of inventory-service's VendorReturnEvent — cross-service Kafka payloads
// in this repo are matched by JSON field name (see MaterialShortageEvent), not a shared
// class, since each service deserializes with useTypeHeaders(false).
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorReturnEvent {
    private Long partId;
    private String skuCode;
    private String partName;
    private String batchNumber;
    private Integer quantity;
    private String ncrNumber;
    private String poNumber;
    private String dispositionedBy;
    private String remarks;
    private LocalDateTime dispositionedAt;
}