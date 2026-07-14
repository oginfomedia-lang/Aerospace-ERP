package com.aerospace.procurement_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Local mirror of inventory-service's QAStatusChangedEvent — cross-service Kafka
// payloads in this repo are matched by JSON field name (see MaterialShortageEvent),
// not a shared class, since each service deserializes with useTypeHeaders(false).
// Only the fields procurement-service actually needs are mirrored here.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QAStatusChangedEvent {
    private Long partId;
    private String skuCode;
    private String batchNumber;
    private String newStatus;
    private Integer quantity;
    private Integer approvedQty;
    private Integer rejectedQty;
    private String poNumber;
    private String ncrNumber;
}
