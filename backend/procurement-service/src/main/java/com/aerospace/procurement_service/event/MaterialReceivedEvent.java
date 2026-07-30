package com.aerospace.procurement_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Local mirror of inventory-service's MaterialReceivedEvent — cross-service Kafka
// payloads in this repo are matched by JSON field name (see MaterialShortageEvent),
// not a shared class, since each service deserializes with useTypeHeaders(false).
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialReceivedEvent {
    private String poNumber;
    private String skuCode;
    private int quantityReceived;
    private String grnNumber;
}
