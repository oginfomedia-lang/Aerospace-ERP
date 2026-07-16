package com.aerospace.production_service.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Local mirror of order-service's WorkOrderEvents.MaterialLineSnapshot — cross-service
// Kafka payloads in this repo are matched by JSON field name (useTypeHeaders(false)),
// not a shared class.
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MaterialLineSnapshot {
    private Long partId;
    private String partName;
    private String partNumber;
    private String batchNumber;
    private Integer quantityRequired;
    private Integer quantityIssued;
}
